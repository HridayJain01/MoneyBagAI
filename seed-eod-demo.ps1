<#
.SYNOPSIS
Creates idempotent BR002 cash activity for the Branch Manager EOD screen.

.DESCRIPTION
Uses the public MoneyBags APIs so account balances, transaction history, ledger
posting, and reporting projections remain consistent. It creates no schema
objects and uses date-scoped idempotency keys, so rerunning it on the same day
does not duplicate transactions.
#>

[CmdletBinding()]
param(
    [string]$GatewayUrl = 'http://127.0.0.1:8090',
    [string]$Username = 'teller2',
    [string]$Password = 'Password@123',
    [string]$AccountNumber = '520000000103',
    [datetime]$BusinessDate = (Get-Date)
)

$ErrorActionPreference = 'Stop'
$gateway = $GatewayUrl.TrimEnd('/')
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$gateway/api/v1/auth/login" `
    -ContentType 'application/json' -Body $loginBody
$authorization = "Bearer $($login.accessToken)"

if ($login.branchCode -ne 'BR002') {
    throw "Seed user $Username is assigned to $($login.branchCode), not BR002."
}

$account = Invoke-RestMethod -Method Get `
    -Uri "$gateway/api/v1/accounts/by-number/$AccountNumber" `
    -Headers @{ Authorization = $authorization }

$dateKey = $BusinessDate.ToString('yyyyMMdd')
$requests = @(
    @{
        Name = 'Deposit 1'
        Type = 'DEPOSIT'
        Path = '/api/v1/transactions/deposits'
        Key = "seed-br002-eod-$dateKey-deposit-01"
        Body = @{
            destinationAccountId = $account.accountId
            amount = 25000
            feeAmount = 0
            currency = 'INR'
            paymentChannel = 'BRANCH'
            paymentMethod = 'CASH'
            narration = 'BR002 EOD seed cash deposit'
        }
    },
    @{
        Name = 'Deposit 2'
        Type = 'DEPOSIT'
        Path = '/api/v1/transactions/deposits'
        Key = "seed-br002-eod-$dateKey-deposit-02"
        Body = @{
            destinationAccountId = $account.accountId
            amount = 8500
            feeAmount = 0
            currency = 'INR'
            paymentChannel = 'BRANCH'
            paymentMethod = 'CASH'
            narration = 'BR002 EOD seed counter deposit'
        }
    },
    @{
        Name = 'Withdrawal'
        Type = 'WITHDRAWAL'
        Path = '/api/v1/transactions/withdrawals'
        Key = "seed-br002-eod-$dateKey-withdrawal-01"
        Body = @{
            sourceAccountId = $account.accountId
            amount = 4000
            feeAmount = 0
            currency = 'INR'
            paymentChannel = 'BRANCH'
            paymentMethod = 'CASH'
            narration = 'BR002 EOD seed cash withdrawal'
        }
    }
)

$from = [uri]::EscapeDataString($BusinessDate.ToString('yyyy-MM-dd') + 'T00:00:00.000Z')
$to = [uri]::EscapeDataString($BusinessDate.Date.AddDays(1).ToString('yyyy-MM-dd') + 'T00:00:00.000Z')
$page = Invoke-RestMethod -Method Get `
    -Uri "$gateway/api/v1/transactions?from=$from&to=$to&page=0&size=200" `
    -Headers @{ Authorization = $authorization }
$existingTransactions = @($page.content | Where-Object {
    $_.branchCode -eq 'BR002' -and $_.makerEmployeeId -eq $login.employeeId
})

$created = foreach ($request in $requests) {
    $existing = $existingTransactions | Where-Object {
        $_.type -eq $request.Type -and
        $_.narration -eq $request.Body.narration -and
        [decimal]$_.amount -eq [decimal]$request.Body.amount
    } | Select-Object -First 1
    if ($existing) {
        [pscustomobject]@{
            Name = $request.Name
            Id = $existing.transactionId
            Reference = $existing.transactionReference
            Amount = $existing.amount
            Status = $existing.status
        }
        continue
    }

    $headers = @{
        Authorization = $authorization
        'Idempotency-Key' = $request.Key
    }
    $transaction = Invoke-RestMethod -Method Post -Uri "$gateway$($request.Path)" `
        -Headers $headers -ContentType 'application/json' `
        -Body ($request.Body | ConvertTo-Json)
    [pscustomobject]@{
        Name = $request.Name
        Id = $transaction.id
        Reference = $transaction.reference
        Amount = $transaction.amount
        Status = $transaction.status
    }
}

foreach ($row in $created) {
    for ($attempt = 0; $attempt -lt 12; $attempt++) {
        $status = Invoke-RestMethod -Method Get `
            -Uri "$gateway/api/v1/transactions/$($row.Id)/status" `
            -Headers @{ Authorization = $authorization }
        $row.Status = $status.status
        if ($status.status -in @('COMPLETED', 'FAILED', 'REJECTED', 'CANCELLED', 'REVERSED')) {
            break
        }
        Start-Sleep -Seconds 1
    }
}

$created | Format-Table Name, Reference, Amount, Status -AutoSize
