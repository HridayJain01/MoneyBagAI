<#
.SYNOPSIS
Stops every MoneyBags service started by run-all.ps1.

.DESCRIPTION
Stops processes recorded in .moneybags-pids first, then sweeps any service port still
listening -- a jar started by hand leaves no pid file but still holds its port.
#>

[CmdletBinding()]
param(
    [switch]$Force
)

$ErrorActionPreference = 'Continue'

$projectRoot  = $PSScriptRoot
$pidDirectory = Join-Path $projectRoot '.moneybags-pids'

# Reverse of the startup order: edge first, registry last.
$services = @(
    @{ Name = 'api-gateway';                 Port = 8090 },
    @{ Name = 'statement-reporting-service'; Port = 8086 },
    @{ Name = 'transaction-service';         Port = 8084 },
    @{ Name = 'account-service';             Port = 8083 },
    @{ Name = 'audit-service';               Port = 8091 },
    @{ Name = 'notification-service';        Port = 8089 },
    @{ Name = 'ledger-service';              Port = 8085 },
    @{ Name = 'customer-service';            Port = 8082 },
    @{ Name = 'configuration-service';       Port = 8092 },
    @{ Name = 'branch-employee-service';     Port = 8081 },
    @{ Name = 'product-service';             Port = 8088 },
    @{ Name = 'identity-service';            Port = 8087 },
    @{ Name = 'eureka-server';               Port = 8080 }
)

function Stop-ByPidFile {
    param([string]$Name)

    $pidFile = Join-Path $pidDirectory "$Name.json"
    if (-not (Test-Path $pidFile)) { return $false }

    try {
        $record = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
        $process = Get-Process -Id $record.ProcessId -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $record.ProcessId -Force:$Force -ErrorAction Stop
            Write-Host "  stopped $Name (pid $($record.ProcessId))" -ForegroundColor Green
        } else {
            Write-Host "  $Name was not running (stale pid file)" -ForegroundColor DarkGray
        }
        Remove-Item -LiteralPath $pidFile -ErrorAction SilentlyContinue
        return $true
    } catch {
        Write-Host "  could not stop $Name : $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

function Stop-ByPort {
    param([string]$Name, [int]$Port)

    $owners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $owners) { return }

    foreach ($owner in $owners) {
        $process = Get-Process -Id $owner.OwningProcess -ErrorAction SilentlyContinue
        # Only ever kill a JVM. Something else on the port is not ours to stop.
        if ($process -and $process.ProcessName -eq 'java') {
            try {
                Stop-Process -Id $process.Id -Force:$Force -ErrorAction Stop
                Write-Host "  stopped $Name on port $Port (pid $($process.Id))" -ForegroundColor Green
            } catch {
                Write-Host "  could not stop pid $($process.Id) on $Port : $($_.Exception.Message)" -ForegroundColor Yellow
            }
        } elseif ($process) {
            Write-Host "  port $Port is held by $($process.ProcessName) (pid $($process.Id)); leaving it alone" -ForegroundColor Yellow
        }
    }
}

Write-Host ''
Write-Host 'Stopping MoneyBags services' -ForegroundColor Cyan

foreach ($service in $services) {
    Stop-ByPidFile -Name $service.Name | Out-Null
    Stop-ByPort -Name $service.Name -Port $service.Port
}

if ((Test-Path $pidDirectory) -and -not (Get-ChildItem -Path $pidDirectory -ErrorAction SilentlyContinue)) {
    Remove-Item -Path $pidDirectory -Recurse -ErrorAction SilentlyContinue
}

Write-Host 'Done.' -ForegroundColor Green
