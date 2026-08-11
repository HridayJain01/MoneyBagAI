<#
.SYNOPSIS
Stops the local MoneyBags Eureka, API Gateway and Customer Service processes.

.DESCRIPTION
Stops the exact Maven process trees recorded by start-moneybags.ps1. If a
service was started before PID tracking was added, -ForceByPort can stop the
process listening on that service's configured port.
#>

[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'Medium')]
param(
    [switch]$ForceByPort,
    [int]$ShutdownTimeoutSeconds = 20
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$pidDirectory = Join-Path $projectRoot '.moneybags-pids'

$services = @(
    [pscustomobject]@{ Name = 'api-gateway'; Port = 8090 },
    [pscustomobject]@{ Name = 'customer-service'; Port = 8082 },
    [pscustomobject]@{ Name = 'eureka-server'; Port = 8080 }
)

function Get-ListeningProcessId {
    param([Parameter(Mandatory)] [int]$Port)

    $listener = & netstat.exe -ano -p tcp |
        Select-String -Pattern (':{0}\s+.*LISTENING\s+(\d+)\s*$' -f $Port) |
        Select-Object -First 1

    if ($null -eq $listener) {
        return $null
    }

    return [int]$listener.Matches[0].Groups[1].Value
}

function Test-RecordedProcess {
    param(
        [Parameter(Mandatory)] [pscustomobject]$Record,
        [Parameter(Mandatory)] [System.Diagnostics.Process]$Process
    )

    if ($Process.ProcessName -ne $Record.ProcessName) {
        return $false
    }

    $recordedStart = [DateTimeOffset]::Parse($Record.StartedAtUtc).UtcDateTime
    $actualStart = $Process.StartTime.ToUniversalTime()
    return [Math]::Abs(($actualStart - $recordedStart).TotalSeconds) -lt 2
}

function Stop-ProcessTree {
    param(
        [Parameter(Mandatory)] [int]$ProcessId,
        [Parameter(Mandatory)] [string]$ServiceName
    )

    if (-not $PSCmdlet.ShouldProcess("$ServiceName process tree (PID $ProcessId)", 'Stop')) {
        return $false
    }

    & taskkill.exe /PID $ProcessId /T /F | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to stop $ServiceName process tree (PID $ProcessId)."
    }

    return $true
}

foreach ($service in $services) {
    $pidFile = Join-Path $pidDirectory "$($service.Name).json"
    $stopped = $false

    if (Test-Path -LiteralPath $pidFile) {
        try {
            $record = Get-Content -Raw -LiteralPath $pidFile | ConvertFrom-Json
            $process = Get-Process -Id ([int]$record.ProcessId) -ErrorAction SilentlyContinue

            if ($null -ne $process -and (Test-RecordedProcess -Record $record -Process $process)) {
                $stopped = Stop-ProcessTree `
                    -ProcessId ([int]$record.ProcessId) `
                    -ServiceName $service.Name
            } elseif ($null -ne $process) {
                Write-Warning "Ignoring stale PID record for $($service.Name); PID $($record.ProcessId) now belongs to another process."
            }
        } catch {
            Write-Warning "Could not use PID record for $($service.Name): $($_.Exception.Message)"
        }
    }

    if (-not $stopped) {
        $listeningProcessId = Get-ListeningProcessId -Port $service.Port

        if ($null -eq $listeningProcessId) {
            Write-Host "$($service.Name) is not listening on port $($service.Port)." -ForegroundColor Yellow
            if (Test-Path -LiteralPath $pidFile) {
                if ($PSCmdlet.ShouldProcess($pidFile, 'Remove stale PID record')) {
                    Remove-Item -LiteralPath $pidFile -Force
                }
            }
        } elseif ($ForceByPort) {
            $stopped = Stop-ProcessTree `
                -ProcessId $listeningProcessId `
                -ServiceName "$($service.Name) port fallback"
        } else {
            Write-Warning "$($service.Name) is listening on port $($service.Port), but no valid PID record exists. Re-run with -ForceByPort to stop that listener."
        }
    }

    if ($stopped -and (Test-Path -LiteralPath $pidFile)) {
        Remove-Item -LiteralPath $pidFile -Force
    }
}

if ($WhatIfPreference) {
    Write-Host 'MoneyBags stop dry run completed.' -ForegroundColor Green
    exit 0
}

$deadline = (Get-Date).AddSeconds($ShutdownTimeoutSeconds)
do {
    $remaining = $services | Where-Object {
        $null -ne (Get-ListeningProcessId -Port $_.Port)
    }

    if ($remaining.Count -eq 0) {
        Write-Host 'MoneyBags services stopped.' -ForegroundColor Green
        exit 0
    }

    Start-Sleep -Milliseconds 500
} while ((Get-Date) -lt $deadline)

$remainingDescription = ($remaining | ForEach-Object {
    "$($_.Name):$($_.Port)"
}) -join ', '

throw "Timed out waiting for services to stop: $remainingDescription"
