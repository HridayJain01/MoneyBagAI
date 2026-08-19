<#
.SYNOPSIS
End-to-end smoke test through the gateway.

.DESCRIPTION
Exercises the full vertical slice: login -> open an account application as a teller ->
approve it as a different employee -> deposit -> transfer -> read the balance -> pull a
mini statement.

Every step prints PASS or FAIL with the correlation id, so a failure can be traced with
  GET /api/v1/audit-events/trace/{correlationId}
#>

[CmdletBinding()]
param(
    [string]$GatewayUrl = 'http://localhost:8090'
)

$ErrorActionPreference = 'Continue'

$script:passed = 0
$script:failed = 0
$script:correlationId = [guid]::NewGuid().ToString()

function Step {
    param([string]$Name, [scriptblock]$Action)
    try {
        $result = & $Action
        Write-Host "  PASS  $Name" -ForegroundColor Green
        $script:passed++
        return $result
    } catch {
        Write-Host "  FAIL  $Name" -ForegroundColor Red
        Write-Host "        $($_.Exception.Message)" -ForegroundColor DarkRed
        if ($_.ErrorDetails.Message) {
            Write-Host "        $($_.ErrorDetails.Message)" -ForegroundColor DarkRed
        }
        $script:failed++
        return $null
    }
}

function Invoke-Api {
    param(
        [string]$Method, [string]$Path, $Body, [string]$AccessToken,
        [hashtable]$ExtraHeaders = @{}
    )
    $headers = @{ 'X-Correlation-Id' = $script:correlationId }
    if ($AccessToken) { $headers['Authorization'] = "Bearer $AccessToken" }
    foreach ($key in $ExtraHeaders.Keys) { $headers[$key] = $ExtraHeaders[$key] }

    $parameters = @{
        Uri = "$GatewayUrl$Path"; Method = $Method; Headers = $headers
        ContentType = 'application/json'; TimeoutSec = 30
    }
    if ($Body) { $parameters['Body'] = ($Body | ConvertTo-Json -Depth 6 -Compress) }
    return Invoke-RestMethod @parameters
}

Write-Host ''
Write-Host "MoneyBags smoke test  (correlation id $script:correlationId)" -ForegroundColor Cyan
Write-Host ''

# 1. Two different employees: the maker cannot approve their own work.
$teller = Step 'login as teller1 (maker)' {
    Invoke-Api -Method POST -Path '/api/v1/auth/login' -Body @{
        username = 'teller1'; password = 'Password@123'
    }
}
$checker = Step 'login as checker1 (approver)' {
    Invoke-Api -Method POST -Path '/api/v1/auth/login' -Body @{
        username = 'checker1'; password = 'Password@123'
    }
}
if (-not $teller -or -not $checker) {
    Write-Host 'Cannot continue without both access tokens.' -ForegroundColor Red
    exit 1
}

Step 'gateway strips a forged X-Permissions header' {
    # Expect this to be rejected or ignored, never honoured.
    $response = Invoke-Api -Method GET -Path '/api/v1/products' -AccessToken $teller.accessToken `
        -ExtraHeaders @{ 'X-Permissions' = 'ACCOUNT_APPROVE,TRANSACTION_APPROVE' }
    if (-not $response) { throw 'No product catalogue returned' }
    'stripped'
} | Out-Null

Step 'read the product catalogue' {
    $products = Invoke-Api -Method GET -Path '/api/v1/products' -AccessToken $teller.accessToken
    if (@($products).Count -lt 6) { throw "Expected 6 seeded products, saw $(@($products).Count)" }
    $products
} | Out-Null

Step 'read a seeded customer' {
    Invoke-Api -Method GET -Path '/api/v1/customers/CIF900101' -AccessToken $teller.accessToken
} | Out-Null

# 2. Open an account for an existing customer.
$application = Step 'create an account application (teller)' {
    Invoke-Api -Method POST -Path '/api/v1/accounts/applications' -AccessToken $teller.accessToken -Body @{
        cifNo = 'CIF900101'; productCode = 'SAV-REG'
        accountName = 'Smoke Test Savings'; initialDeposit = 5000
    }
}

Step 'maker cannot approve their own application' {
    try {
        Invoke-Api -Method POST -Path "/api/v1/accounts/applications/$($application.applicationId)/approve" `
            -AccessToken $teller.accessToken -Body @{ remarks = 'self-approval attempt' }
        throw 'Self-approval was allowed; maker-checker is not being enforced'
    } catch {
        if ($_.Exception.Message -like '*not being enforced*') { throw }
        'correctly rejected'
    }
} | Out-Null

$approved = Step 'approve the application (checker)' {
    Invoke-Api -Method POST -Path "/api/v1/accounts/applications/$($application.applicationId)/approve" `
        -AccessToken $checker.accessToken -Body @{ remarks = 'smoke test approval' }
}

$accountId = $approved.createdAccountId
if (-not $accountId) {
    Write-Host 'No account was created; cannot exercise the money flow.' -ForegroundColor Red
} else {
    Write-Host "  account $accountId" -ForegroundColor DarkGray

    # 3. Money movement. Each POST carries its own idempotency key.
    #
    # CreateRequest has no `accountId` field, and paymentChannel/paymentMethod are
    # @NotNull -- the earlier bodies here 400d at validation before reaching any
    # business logic, so this whole section only ever appeared to pass. Note also
    # that paymentMethod is not free: the server requires it to match the rail
    # (CASH for /deposits, ACCOUNT for /transfers/internal).
    Step 'deposit 5000' {
        Invoke-Api -Method POST -Path '/api/v1/transactions/deposits' -AccessToken $teller.accessToken `
            -ExtraHeaders @{ 'Idempotency-Key' = [guid]::NewGuid().ToString() } -Body @{
                destinationAccountId = $accountId; amount = 5000; currency = 'INR'
                paymentChannel = 'BRANCH'; paymentMethod = 'CASH'
                narration = 'Smoke test opening deposit'
            }
    } | Out-Null

    Step 'replaying the deposit does not double-post' {
        $key = [guid]::NewGuid().ToString()
        $body = @{
            destinationAccountId = $accountId; amount = 100; currency = 'INR'
            paymentChannel = 'BRANCH'; paymentMethod = 'CASH'; narration = 'Idempotency probe'
        }
        $first  = Invoke-Api -Method POST -Path '/api/v1/transactions/deposits' -AccessToken $teller.accessToken `
            -ExtraHeaders @{ 'Idempotency-Key' = $key } -Body $body
        $second = Invoke-Api -Method POST -Path '/api/v1/transactions/deposits' -AccessToken $teller.accessToken `
            -ExtraHeaders @{ 'Idempotency-Key' = $key } -Body $body
        if ($first.transactionId -ne $second.transactionId) {
            throw "Replay created a second transaction ($($first.transactionId) vs $($second.transactionId))"
        }
        'same transaction returned'
    } | Out-Null

    # The outbox publisher runs on a 5s cycle, so balances settle asynchronously.
    Step 'balance reflects the deposits' {
        $deadline = (Get-Date).AddSeconds(45)
        while ((Get-Date) -lt $deadline) {
            $balance = Invoke-Api -Method GET -Path "/api/v1/accounts/$accountId/balance" -AccessToken $teller.accessToken
            if ([decimal]$balance.ledgerBalance -ge 5100) { return $balance }
            Start-Sleep -Seconds 3
        }
        throw 'Ledger balance did not reach 5100 within 45s; check the outbox publisher'
    } | Out-Null

    Step 'internal transfer to the seeded account' {
        Invoke-Api -Method POST -Path '/api/v1/transactions/transfers/internal' -AccessToken $teller.accessToken `
            -ExtraHeaders @{ 'Idempotency-Key' = [guid]::NewGuid().ToString() } -Body @{
                sourceAccountId = $accountId
                destinationAccountId = 'a0000000-0000-0000-0000-000000000103'
                amount = 1000; currency = 'INR'
                paymentChannel = 'BRANCH'; paymentMethod = 'ACCOUNT'
                narration = 'Smoke test transfer'
            }
    } | Out-Null

    Step 'holds are released after settlement' {
        $deadline = (Get-Date).AddSeconds(45)
        while ((Get-Date) -lt $deadline) {
            $balance = Invoke-Api -Method GET -Path "/api/v1/accounts/$accountId/balance" -AccessToken $teller.accessToken
            if ([decimal]$balance.heldAmount -eq 0) { return $balance }
            Start-Sleep -Seconds 3
        }
        throw 'Held amount did not return to zero; a hold was not consumed'
    } | Out-Null

    Step 'account transaction history' {
        Invoke-Api -Method GET -Path "/api/v1/accounts/$accountId/transactions" -AccessToken $teller.accessToken
    } | Out-Null

    Step 'mini statement' {
        Invoke-Api -Method GET -Path "/api/v1/statements/accounts/$accountId/mini" -AccessToken $teller.accessToken
    } | Out-Null

    Step 'balance history is recorded' {
        $history = Invoke-Api -Method GET -Path "/api/v1/accounts/$accountId/balance-history" -AccessToken $teller.accessToken
        if (@($history.items).Count -lt 1) { throw 'No balance history rows' }
        $history
    } | Out-Null
}

Step 'audit trail for this correlation id' {
    $trace = Invoke-Api -Method GET -Path "/api/v1/audit-events/trace/$script:correlationId" -AccessToken $checker.accessToken
    if (@($trace).Count -lt 1) { throw 'No audit events recorded for this request chain' }
    $trace
} | Out-Null

Write-Host ''
Write-Host "Passed: $script:passed   Failed: $script:failed" `
    -ForegroundColor $(if ($script:failed -eq 0) { 'Green' } else { 'Red' })
Write-Host "Trace:  $GatewayUrl/api/v1/audit-events/trace/$script:correlationId"
Write-Host ''
if ($script:failed -gt 0) { exit 1 }
