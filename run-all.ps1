<#
.SYNOPSIS
Builds and starts the entire MoneyBags system with one command.

.DESCRIPTION
Supersedes start.ps1, which broke when the modules moved under services/ and which
gated on a TCP listener rather than on readiness.

Phases:
  0. Preflight   - java 17+, maven, MySQL reachable
  1. Schemas     - CREATE DATABASE IF NOT EXISTS for all 12 schemas
  2. Build       - one reactor build from the root aggregator pom
  3. Start       - health-gated waves, launching built jars
  4. Smoke       - optional end-to-end flow

.EXAMPLE
.\run-all.ps1
.\run-all.ps1 -Reset -Smoke
.\run-all.ps1 -SkipBuild
#>

[CmdletBinding()]
param(
    [switch]$Reset,
    [switch]$SkipBuild,
    [switch]$Smoke,
    [string]$MySqlHost = 'localhost',
    [int]$MySqlPort = 3306,
    [string]$MySqlUser = 'root',
    [string]$MySqlPassword = 'jheel',
    [int]$HealthTimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'

# MySQL writes its password-on-command-line warning to stderr even when the
# command succeeds. Do not let PowerShell 7 convert native stderr into a
# terminating NativeCommandError; native commands below check $LASTEXITCODE
# where a failure must stop the launcher.
if ($PSVersionTable.PSVersion.Major -ge 7) {
    $PSNativeCommandUseErrorActionPreference = $false
}

# Some Windows IDE sessions expose both PATH and Path. Start-Process rejects that
# duplicate case-insensitive key, so collapse to one canonical value. (Carried over
# from start.ps1 -- it is a real problem, not a leftover.)
$pathValue = [Environment]::GetEnvironmentVariable('Path', 'Process')
[Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
[Environment]::SetEnvironmentVariable('Path', $pathValue, 'Process')

$projectRoot   = $PSScriptRoot
$servicesRoot  = Join-Path $projectRoot 'services'
$logDirectory  = Join-Path $projectRoot 'logs'
$pidDirectory  = Join-Path $projectRoot '.moneybags-pids'

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) { return 0 }

    $loaded = 0
    foreach ($rawLine in Get-Content -LiteralPath $Path) {
        $line = $rawLine.Trim()
        if (-not $line -or $line.StartsWith('#')) { continue }
        if ($line.StartsWith('export ')) { $line = $line.Substring(7).Trim() }

        $separator = $line.IndexOf('=')
        if ($separator -lt 1) { continue }
        $name = $line.Substring(0, $separator).Trim()
        if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
            throw "Invalid environment variable name '$name' in $Path"
        }

        $value = $line.Substring($separator + 1).Trim()
        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
             ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        # The root .env is the single source of truth for local launches. Always
        # refresh the process value so edits are picked up in the same shell.
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        $loaded++
    }
    return $loaded
}

$envFile = Join-Path $projectRoot '.env'
$loadedEnvironmentVariables = Import-DotEnv -Path $envFile
if ($loadedEnvironmentVariables -gt 0) {
    Write-Host "Loaded $loadedEnvironmentVariables values from $envFile" -ForegroundColor DarkGray
}

if (-not $PSBoundParameters.ContainsKey('MySqlHost') -and $env:DB_HOST) {
    $MySqlHost = $env:DB_HOST
}
if (-not $PSBoundParameters.ContainsKey('MySqlPort') -and $env:DB_PORT) {
    $MySqlPort = [int]$env:DB_PORT
}
if (-not $PSBoundParameters.ContainsKey('MySqlUser') -and $env:DB_USERNAME) {
    $MySqlUser = $env:DB_USERNAME
}
if (-not $PSBoundParameters.ContainsKey('MySqlPassword') -and $env:DB_PASSWORD) {
    $MySqlPassword = $env:DB_PASSWORD
}

function Get-EnvironmentPort {
    param([string]$Name, [int]$Default)
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    return $(if ($value) { [int]$value } else { $Default })
}

$eurekaServerPort = Get-EnvironmentPort 'EUREKA_SERVER_PORT' 8080
$apiGatewayPort = Get-EnvironmentPort 'API_GATEWAY_PORT' 8090
$javaExecutable = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java.exe -ErrorAction Stop).Source
}

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $pidDirectory -Force | Out-Null

# Wave order encodes real dependencies: account-service must be up before
# transaction-service can move money, and the gateway must be last so its route table
# resolves against a populated registry.
$waves = @(
    @{ Name = 'Registry';    Services = @(
        @{ Name = 'eureka-server'; Port = $eurekaServerPort }) },
    @{ Name = 'Foundation';  Services = @(
        @{ Name = 'identity-service';      Port = Get-EnvironmentPort 'IDENTITY_SERVICE_PORT' 8087 },
        @{ Name = 'product-service';       Port = Get-EnvironmentPort 'PRODUCT_SERVICE_PORT' 8088 },
        @{ Name = 'branch-employee-service'; Port = Get-EnvironmentPort 'BRANCH_SERVICE_PORT' 8081 },
        @{ Name = 'configuration-service'; Port = Get-EnvironmentPort 'CONFIG_SERVICE_PORT' 8092 },
        @{ Name = 'customer-service';      Port = Get-EnvironmentPort 'CUSTOMER_SERVICE_PORT' 8082 },
        @{ Name = 'kyc-service';           Port = Get-EnvironmentPort 'KYC_SERVICE_PORT' 8093 },
        @{ Name = 'ledger-service';        Port = Get-EnvironmentPort 'LEDGER_SERVICE_PORT' 8085 },
        @{ Name = 'notification-service';  Port = Get-EnvironmentPort 'NOTIFICATION_SERVICE_PORT' 8089 },
        @{ Name = 'audit-service';         Port = Get-EnvironmentPort 'AUDIT_SERVICE_PORT' 8091 }) },
    @{ Name = 'Accounts';    Services = @(
        @{ Name = 'account-service'; Port = Get-EnvironmentPort 'ACCOUNT_SERVICE_PORT' 8083 }) },
    @{ Name = 'Financial';   Services = @(
        @{ Name = 'transaction-service';         Port = Get-EnvironmentPort 'TRANSACTION_SERVICE_PORT' 8084 },
        @{ Name = 'statement-reporting-service'; Port = Get-EnvironmentPort 'STATEMENT_SERVICE_PORT' 8086 }) },
    @{ Name = 'Edge';        Services = @(
        @{ Name = 'api-gateway'; Port = $apiGatewayPort }) }
)

# One schema per service, all under the moneybags_ prefix so -Reset can drop the whole
# system without touching anything else on the developer's MySQL instance.
$schemas = @(
    'moneybags_identity', 'moneybags_customer', 'moneybags_branch',
    'moneybags_configuration', 'moneybags_product', 'moneybags_account',
    'moneybags_transaction', 'moneybags_ledger', 'moneybags_statement',
    'moneybags_notification', 'moneybags_audit', 'moneybags_kyc'
)

function Write-Phase {
    param([string]$Message)
    Write-Host ''
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Find-MySqlClient {
    $candidate = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($candidate) { return $candidate.Source }
    $known = @(
        'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
    )
    foreach ($path in $known) { if (Test-Path $path) { return $path } }
    return $null
}

function Test-TcpPort {
    param([string]$TargetHost, [int]$Port, [int]$TimeoutMs = 2000)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($TargetHost, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs)) { return $false }
        $client.EndConnect($async)
        return $true
    } catch { return $false } finally { $client.Close() }
}

function Invoke-MySqlStatement {
    param([string]$MySqlPath, [string]$Statement)

    # Run mysql.exe through .NET directly rather than the PowerShell call operator:
    # PowerShell 5.1 turns a native command's stderr into ErrorRecords, and with
    # $ErrorActionPreference = 'Stop' mysql's routine password warning would abort
    # the whole script on an otherwise successful statement.
    #
    # ProcessStartInfo.ArgumentList is NOT available here -- it arrived in .NET Core
    # 2.1, and Windows PowerShell 5.1 runs on .NET Framework 4.x, where the property
    # is null. Use the Arguments string instead.
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $MySqlPath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.Arguments = '-h "{0}" -P {1} -u "{2}" -e "{3}"' -f `
        $MySqlHost, $MySqlPort, $MySqlUser, $Statement.Replace('"', '\"')

    # MYSQL_PWD keeps the password off the command line, which is both safer than
    # -pPASSWORD and the reason mysql stops emitting its warning at all.
    $startInfo.EnvironmentVariables['MYSQL_PWD'] = $MySqlPassword

    $mysqlProcess = New-Object System.Diagnostics.Process
    $mysqlProcess.StartInfo = $startInfo
    [void]$mysqlProcess.Start()
    $mysqlOutput = $mysqlProcess.StandardOutput.ReadToEnd()
    $mysqlError = $mysqlProcess.StandardError.ReadToEnd()
    $mysqlProcess.WaitForExit()
    $exitCode = $mysqlProcess.ExitCode
    $mysqlProcess.Dispose()

    if ($exitCode -ne 0) {
        throw "MySQL statement failed (exit $exitCode): $mysqlError"
    }
    return $mysqlOutput
}

# ---------------------------------------------------------------- Phase 0
Write-Phase 'Phase 0: preflight'

# java -version writes its version string to stderr. Redirect within cmd.exe so
# PowerShell does not turn that expected output into a NativeCommandError when
# $ErrorActionPreference is Stop.
$javaVersionRaw = (& cmd.exe /d /c 'java -version 2>&1' | Select-Object -First 1)
if ($javaVersionRaw -notmatch '"(\d+)') {
    throw "Could not determine the Java version. Install JDK 17+ and ensure java is on PATH."
}
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 17) { throw "Java 17+ is required; found $javaMajor. Install a newer JDK." }
Write-Host "  Java $javaMajor" -ForegroundColor Green

if (-not (Get-Command mvn.cmd -ErrorAction SilentlyContinue)) {
    throw "Maven is not on PATH. Install Maven, or add its bin directory to PATH."
}
Write-Host '  Maven found' -ForegroundColor Green

if (-not (Test-TcpPort -TargetHost $MySqlHost -Port $MySqlPort)) {
    throw "MySQL is not reachable on ${MySqlHost}:${MySqlPort}. Start the MySQL service and retry."
}
Write-Host "  MySQL reachable on ${MySqlHost}:${MySqlPort}" -ForegroundColor Green

# Java's NIO selector opens its internal pipe over an AF_UNIX socket. If that is broken
# on this machine, every Tomcat and Netty server fails with "Unable to establish loopback
# connection" -- so check it here rather than letting eleven services fail one by one.
$loopbackProbe = Join-Path $env:TEMP 'MoneyBagsLoopbackProbe.java'
$loopbackProbeSource = @'
import java.nio.channels.Selector;
public class MoneyBagsLoopbackProbe {
    public static void main(String[] a) throws Exception {
        try (Selector s = Selector.open()) { System.out.println("SELECTOR_OK"); }
    }
}
'@
# Windows PowerShell's -Encoding utf8 writes a BOM. javac can interpret that
# as source text under the platform code page, so write UTF-8 explicitly with
# no byte-order mark.
[System.IO.File]::WriteAllText(
    $loopbackProbe,
    $loopbackProbeSource,
    [System.Text.UTF8Encoding]::new($false)
)
# Keep Java's diagnostic output available for the user-facing failure below.
# cmd.exe performs the redirection before PowerShell sees stderr, avoiding a
# NativeCommandError when the selector probe intentionally exposes the fault.
$probeOutput = & cmd.exe /d /c ('java "{0}" 2>&1' -f $loopbackProbe) | Out-String
Remove-Item $loopbackProbe -ErrorAction SilentlyContinue
if ($probeOutput -notmatch 'SELECTOR_OK') {
    Write-Host $probeOutput -ForegroundColor Red
    throw @"
This machine cannot open a Java NIO selector, so no service can start a web server.
Usually a broken Winsock AF_UNIX provider. Fix with an elevated prompt, then reboot:
    netsh winsock reset
"@
}
Write-Host '  NIO loopback OK' -ForegroundColor Green

# ---------------------------------------------------------------- Phase 1
Write-Phase 'Phase 1: schema bootstrap'

$mysql = Find-MySqlClient
if ($mysql) {
    if ($Reset) {
        Write-Host '  -Reset: dropping and recreating all moneybags_* schemas' -ForegroundColor Yellow
        foreach ($schema in $schemas) {
            Invoke-MySqlStatement -MySqlPath $mysql -Statement "DROP DATABASE IF EXISTS ``$schema``;" | Out-Null
        }
    }
    foreach ($schema in $schemas) {
        Invoke-MySqlStatement -MySqlPath $mysql -Statement `
            "CREATE DATABASE IF NOT EXISTS ``$schema`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
    }
    Write-Host "  $($schemas.Count) schemas ready" -ForegroundColor Green
} else {
    # Not fatal: every JDBC URL also carries createDatabaseIfNotExist=true.
    Write-Host '  mysql.exe not found; relying on createDatabaseIfNotExist in the JDBC URLs' -ForegroundColor Yellow
    if ($Reset) { throw "-Reset needs the mysql client. Add MySQL's bin directory to PATH." }
}

# ---------------------------------------------------------------- Phase 2
if (-not $SkipBuild) {
    Write-Phase 'Phase 2: build'
    $buildLog = Join-Path $logDirectory 'build.log'
    # A previous or interrupted launcher run can leave services holding their
    # executable JARs open on Windows. stop.ps1 checks both PID records and the
    # known service ports, so it also catches a process started just before its
    # PID record could be written.
    Write-Host '  stopping services from the previous run to release JAR files'
    & (Join-Path $projectRoot 'stop.ps1') -Force
    Write-Host "  mvn -T 1C -Dmaven.test.skip=true clean install  (log: $buildLog)"
    # Maven writes progress to stderr, which PowerShell 5.1 would turn into terminating
    # ErrorRecords under 'Stop'. Relax the preference for this call and judge success by
    # the exit code, which is the only thing that actually means the build worked.
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & cmd.exe /d /c "mvn.cmd -f `"$(Join-Path $projectRoot 'pom.xml')`" -T 1C -Dmaven.test.skip=true clean install 2>&1" |
            Tee-Object -FilePath $buildLog | Out-Null
        $buildExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($buildExitCode -ne 0) {
        throw "Build failed (exit $buildExitCode). See $buildLog. Not starting services against stale jars."
    }
    Write-Host '  Build succeeded' -ForegroundColor Green
} else {
    Write-Phase 'Phase 2: build skipped'
}

# ---------------------------------------------------------------- Phase 3
Write-Phase 'Phase 3: starting services'

function Start-MoneyBagsService {
    param([string]$Name, [int]$Port)

    if (Test-TcpPort -TargetHost 'localhost' -Port $Port -TimeoutMs 500) {
        Write-Host "  $Name already listening on $Port; skipping" -ForegroundColor Yellow
        return
    }

    $serviceDirectory = Join-Path $servicesRoot $Name
    # Exclude *.jar.original, which the spring-boot plugin leaves beside the fat jar.
    $jar = Get-ChildItem -Path (Join-Path $serviceDirectory 'target') -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $jar) { throw "No jar for $Name. Run without -SkipBuild." }

    # Built jars rather than `mvn spring-boot:run`: one JVM per service instead of two,
    # and startup is markedly faster.
    #
    # The jar path is quoted because Start-Process joins -ArgumentList into a single
    # command line. This repository lives under "C:\Users\Hriday Jain\...", so an
    # unquoted path would reach java as two separate arguments.
    $process = Start-Process -FilePath $javaExecutable `
        -ArgumentList @('-jar', ('"{0}"' -f $jar.FullName)) `
        -WorkingDirectory $serviceDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDirectory "$Name.out.log") `
        -RedirectStandardError  (Join-Path $logDirectory "$Name.err.log") `
        -PassThru

    # stop.ps1 removes this directory when its final PID file is deleted.
    # Recreate it here so a stop/build/start cycle can always persist new PIDs.
    New-Item -ItemType Directory -Path $pidDirectory -Force | Out-Null
    [pscustomobject]@{
        Name = $Name; ProcessId = $process.Id; Port = $Port
        StartedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $pidDirectory "$Name.json") -Encoding UTF8

    Write-Host "  started $Name (pid $($process.Id), port $Port)"
}

function Wait-ForHealth {
    param([string]$Name, [int]$Port, [int]$TimeoutSeconds)

    # Gate on /actuator/health, NOT on the socket being bound. A bound socket only means
    # Tomcat started; Flyway may still be migrating, and a dependent service that races
    # ahead will fail against a half-built schema.
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" `
                -TimeoutSec 5 -ErrorAction Stop
            if ($response.status -eq 'UP') {
                Write-Host "  $Name is UP" -ForegroundColor Green
                return $true
            }
        } catch { }

        # Do not spend the full health timeout on a JVM that already failed.
        # Spring writes application startup failures to the redirected stdout log.
        $pidFile = Join-Path $pidDirectory "$Name.json"
        if (Test-Path -LiteralPath $pidFile) {
            $record = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
            if (-not (Get-Process -Id $record.ProcessId -ErrorAction SilentlyContinue)) {
                Write-Host "  $Name exited during startup - see logs\$Name.out.log" -ForegroundColor Red
                Remove-Item -LiteralPath $pidFile -ErrorAction SilentlyContinue
                return $false
            }
        }
        Start-Sleep -Seconds 3
    }
    Write-Host "  $Name did not become healthy in ${TimeoutSeconds}s - see logs\$Name.out.log" -ForegroundColor Red
    return $false
}

$failed = @()
foreach ($wave in $waves) {
    Write-Host "Wave: $($wave.Name)" -ForegroundColor Magenta
    foreach ($service in $wave.Services) {
        Start-MoneyBagsService -Name $service.Name -Port $service.Port
    }
    foreach ($service in $wave.Services) {
        if (-not (Wait-ForHealth -Name $service.Name -Port $service.Port -TimeoutSeconds $HealthTimeoutSeconds)) {
            $failed += $service.Name
        }
    }
}

# Eureka registration lags readiness, and lb:// routes 503 until the gateway's client has
# fetched the registry. Without this wait the first request looks like a bug.
Write-Phase 'Waiting for Eureka registrations'
$expected = ($waves | ForEach-Object { $_.Services } | ForEach-Object { $_.Name }) |
    Where-Object { $_ -ne 'eureka-server' }
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    try {
        $registry = Invoke-RestMethod -Uri "http://localhost:$eurekaServerPort/eureka/apps" `
            -Headers @{ Accept = 'application/json' } -TimeoutSec 5
        $registered = @($registry.applications.application | ForEach-Object { $_.name })
        # identity-service registers under its Eureka id, not its module name.
        $missing = @($expected | Where-Object {
            $probe = if ($_ -eq 'identity-service') { 'SECURITY-SERVICE' } else { $_.ToUpper() }
            $registered -notcontains $probe
        })
        if ($missing.Count -eq 0) {
            Write-Host "  all $($expected.Count) services registered" -ForegroundColor Green
            break
        }
    } catch { }
    Start-Sleep -Seconds 3
}

Write-Phase 'Summary'
if ($failed.Count -gt 0) {
    Write-Host "Unhealthy: $($failed -join ', ')" -ForegroundColor Red
} else {
    Write-Host 'All services healthy.' -ForegroundColor Green
}
Write-Host ''
Write-Host "  Gateway      http://localhost:$apiGatewayPort"
Write-Host "  Swagger UI   http://localhost:$apiGatewayPort/swagger-ui.html"
Write-Host "  Eureka       http://localhost:$eurekaServerPort"
Write-Host "  Logs         $logDirectory"
Write-Host ''
Write-Host "  Sign in:  POST http://localhost:$apiGatewayPort/api/v1/auth/login"
Write-Host '            {"username":"teller1","password":"Password@123"}'
Write-Host '            then send  Authorization: Bearer <accessToken>'

# ---------------------------------------------------------------- Phase 4
if ($Smoke) {
    Write-Phase 'Phase 4: smoke test'
    & (Join-Path $projectRoot 'smoke-test.ps1')
}
