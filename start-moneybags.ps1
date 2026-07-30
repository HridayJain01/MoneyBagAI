<#!
.SYNOPSIS
Starts the MoneyBags microservices locally, beginning with Eureka.

.DESCRIPTION
Each service is launched in a background Maven process. Output and errors are
written to the logs directory beside this script.
#>

[CmdletBinding()]
param(
    [int]$EurekaStartupTimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$logDirectory = Join-Path $projectRoot 'logs'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

function Test-PortListening {
    param([int]$Port)

    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-MoneyBagsService {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)] [int]$Port
    )

    if (Test-PortListening -Port $Port) {
        Write-Host "$Name is already listening on port $Port; skipping." -ForegroundColor Yellow
        return
    }

    $serviceDirectory = Join-Path $projectRoot $Name
    $pomFile = Join-Path $serviceDirectory 'pom.xml'
    if (-not (Test-Path $pomFile)) {
        throw "Cannot find $pomFile"
    }

    $standardOutput = Join-Path $logDirectory "$Name.out.log"
    $standardError = Join-Path $logDirectory "$Name.err.log"
    $process = Start-Process -FilePath 'mvn.cmd' `
        -ArgumentList '-f', $pomFile, 'spring-boot:run' `
        -WorkingDirectory $serviceDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $standardOutput `
        -RedirectStandardError $standardError `
        -PassThru

    Write-Host "Started $Name (PID $($process.Id), port $Port)."
}

function Wait-ForPort {
    param(
        [Parameter(Mandatory)] [int]$Port,
        [Parameter(Mandatory)] [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening -Port $Port) {
            Write-Host "Eureka is listening on port $Port." -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "Eureka did not start on port $Port within $TimeoutSeconds seconds. Check logs\\eureka-server.err.log."
}

Start-MoneyBagsService -Name 'eureka-server' -Port 8761
Wait-ForPort -Port 8761 -TimeoutSeconds $EurekaStartupTimeoutSeconds

@(
    @{ Name = 'security-service'; Port = 8081 },
    @{ Name = 'customer-service'; Port = 8082 },
    @{ Name = 'product-service'; Port = 8083 },
    @{ Name = 'account-service'; Port = 8084 },
    @{ Name = 'transaction-service'; Port = 8087 },
    @{ Name = 'statement-service'; Port = 8086 },
    @{ Name = 'api-gateway'; Port = 8090 }
) | ForEach-Object {
    Start-MoneyBagsService -Name $_.Name -Port $_.Port
}

Write-Host "MoneyBags startup requested. Logs: $logDirectory" -ForegroundColor Green
