# backend/deploy/smoke-test.ps1
# Smoke test: register -> login -> /users/me -> create list -> get lists -> add item

$ApiUrl    = "https://d5d8ajpqsbdhvlhmcoga.wnq2w1o5.apigw.yandexcloud.net"
$Timestamp = (Get-Date -Format "yyyyMMddHHmmss")
$Email     = "smoke-test-$Timestamp@example.com"
$Password  = "SmokeTest123!"
$Name      = "Smoke Test User $Timestamp"
$ListTitle = "Smoke test list $Timestamp"
$ItemName  = "Smoke test item $Timestamp"

# -- state --------------------------------------------------------------------
$AccessToken = $null
$UserId      = $null
$ListId      = $null

# -- result tracking ----------------------------------------------------------
$Results    = @()
$TotalStart = Get-Date

Write-Host ""
Write-Host "=== Kartoshka Backend Smoke Test ===" -ForegroundColor Cyan
Write-Host "API:   $ApiUrl"
Write-Host "Email: $Email"
Write-Host ""

# -- helpers ------------------------------------------------------------------
function Run-Step {
    param(
        [int]         $Number,
        [string]      $Label,
        [scriptblock] $Action
    )

    $dots = "." * [Math]::Max(1, 40 - $Label.Length)
    Write-Host -NoNewline "> Step $Number`: $Label $dots"

    $stepStart = Get-Date
    $ok = $false
    try {
        & $Action
        $ok = $true
    } catch {
        $ms = [int](New-TimeSpan -Start $stepStart -End (Get-Date)).TotalMilliseconds
        Write-Host " FAIL ($($ms)ms)" -ForegroundColor Red
        $body = $_.ErrorDetails.Message
        if (-not $body) { $body = $_.Exception.Message }
        Write-Host "   $body" -ForegroundColor Red
    }

    if ($ok) {
        $ms = [int](New-TimeSpan -Start $stepStart -End (Get-Date)).TotalMilliseconds
        Write-Host " OK ($($ms)ms)" -ForegroundColor Green
    }

    $script:Results += [PSCustomObject]@{
        Step   = "Step $Number"
        Label  = $Label
        Result = if ($ok) { "OK" } else { "FAIL" }
        Ms     = [int](New-TimeSpan -Start $stepStart -End (Get-Date)).TotalMilliseconds
    }
}

# -- Step 1: Register ---------------------------------------------------------
Run-Step 1 "Register" {
    $body = @{ email = $Email; password = $Password; name = $Name } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/email/register" `
        -ContentType "application/json" -Body $body
    if (-not $resp.access_token) { throw "No access_token in response" }
    # Сохраняем токен из register, но в Step 2 (login) перезатрём — это намеренно,
    # чтобы убедиться что bcrypt+login реально работают, а не закэшированный токен.
    $script:AccessToken = $resp.access_token
}

# -- Step 2: Login ------------------------------------------------------------
Run-Step 2 "Login" {
    $body = @{ email = $Email; password = $Password } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/email/login" `
        -ContentType "application/json" -Body $body
    if (-not $resp.access_token) { throw "No access_token in response" }
    $script:AccessToken = $resp.access_token
}

# -- Step 3: GET /users/me ----------------------------------------------------
Run-Step 3 "GET /users/me" {
    $headers = @{ Authorization = "Bearer $script:AccessToken" }
    $resp = Invoke-RestMethod -Method Get -Uri "$ApiUrl/users/me" -Headers $headers
    if ($resp.email -ne $Email) { throw "email mismatch: got '$($resp.email)'" }
    $script:UserId = $resp.user_id
}

# -- Step 4: Create list ------------------------------------------------------
Run-Step 4 "Create list" {
    $headers = @{ Authorization = "Bearer $script:AccessToken" }
    $body = @{ title = $ListTitle } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$ApiUrl/lists" `
        -ContentType "application/json" -Headers $headers -Body $body
    if (-not $resp.list_id) { throw "No list_id in response" }
    $script:ListId = $resp.list_id
}

# -- Step 5: GET /lists -------------------------------------------------------
Run-Step 5 "GET /lists" {
    $headers = @{ Authorization = "Bearer $script:AccessToken" }
    $resp = Invoke-RestMethod -Method Get -Uri "$ApiUrl/lists" -Headers $headers
    $found = $resp | Where-Object { $_.list_id -eq $script:ListId }
    if (-not $found) { throw "Created list '$($script:ListId)' not found in GET /lists response" }
}

# -- Step 6: Add item to list -------------------------------------------------
Run-Step 6 "Add item to list" {
    $headers = @{ Authorization = "Bearer $script:AccessToken" }
    $body = @{ name = $ItemName } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$ApiUrl/lists/$($script:ListId)/items" `
        -ContentType "application/json" -Headers $headers -Body $body
    if (-not $resp.item_id) { throw "No item_id in response" }
}

# -- Summary ------------------------------------------------------------------
$totalMs   = [int](New-TimeSpan -Start $TotalStart -End (Get-Date)).TotalMilliseconds
$failCount = ($Results | Where-Object { $_.Result -eq "FAIL" }).Count

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
$Results | ForEach-Object {
    $color = if ($_.Result -eq "OK") { "Green" } else { "Red" }
    $mark  = if ($_.Result -eq "OK") { "OK" } else { "FAIL" }
    Write-Host "  [$mark] $($_.Step): $($_.Label) ($($_.Ms)ms)" -ForegroundColor $color
}
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "All 6 steps passed (total: $($totalMs)ms)" -ForegroundColor Green
    exit 0
} else {
    Write-Host "$failCount of 6 failed (total: $($totalMs)ms)" -ForegroundColor Red
    exit 1
}
