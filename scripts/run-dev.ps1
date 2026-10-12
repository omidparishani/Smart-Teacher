# ─────────────────────────────────────────────────────────────
#  اجرای محیط توسعه Smart Teacher
#  استفاده:  powershell -ExecutionPolicy Bypass -File scripts\run-dev.ps1
# ─────────────────────────────────────────────────────────────
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "==> Smart Teacher — راه‌اندازی توسعه" -ForegroundColor Cyan

# JDK 25 اگر موجود باشد
$jdk25 = "C:\Program Files\Java\jdk-25.0.2"
if (Test-Path $jdk25) { $env:JAVA_HOME = $jdk25 }

# از ریشه پروژه .env را برای Maven/Spring در دسترس کن
if (Test-Path "$Root\.env") {
    Get-Content "$Root\.env" | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $i = $_.IndexOf('='); if ($i -lt 1) { return }
        $k = $_.Substring(0, $i).Trim()
        $v = $_.Substring($i + 1).Trim()
        Set-Item -Path "Env:$k" -Value $v
    }
}

# Postgres روی 5432
$pg = docker ps --format "{{.Names}}" | Where-Object { $_ -match "postgres" }
if (-not $pg) {
    Write-Host "==> postgres نیست — docker compose up -d postgres"
    docker compose up -d postgres
    Start-Sleep -Seconds 5
} else {
    Write-Host "==> postgres در حال اجرا: $pg"
}

# initData تازه برای Mini App (۵ دقیقه اعتبار + یک‌بارمصرف)
#
# 🔴 هیچ توکنی در این فایل نیست — عمدی است. توکن ربات یک راز است و راز در
# مخزن گیت یعنی هر کسی که مخزن را بخواند می‌تواند به‌جای ما پیام بفرستد.
#
# ⚠️ و توکن باید دقیقاً همانی باشد که بک‌اند می‌بیند: `initData` با آن امضا
# می‌شود و `EitaaInitDataValidator` همان امضا را با همان توکن بررسی می‌کند.
# پس هر دو طرف از `.env` می‌خوانند (بک‌اند خودش `.env` را import می‌کند).
$botToken = ""
if (Test-Path ".env") {
    $line = Get-Content ".env" | Where-Object { $_ -match '^(APP_EITAA_BOT_TOKEN|EITAA_BOT_TOKEN)=' } | Select-Object -First 1
    if ($line) {
        $t = ($line -split '=', 2)[1].Trim()
        if ($t) { $botToken = $t }
    }
}
if (-not $botToken) {
    Write-Host "توکن ربات ایتا پیدا نشد." -ForegroundColor Red
    Write-Host "  APP_EITAA_BOT_TOKEN را در فایل .env ریشه پروژه بگذارید (نمونه: .env.example)." -ForegroundColor Yellow
    Write-Host "  (صورت کوتاه EITAA_BOT_TOKEN هم پذیرفته می‌شود.)" -ForegroundColor Yellow
    Write-Host "  راهنمای توکن و API: docs/eitaa.md" -ForegroundColor Yellow
    exit 1
}
& "$PSScriptRoot\New-DevInitData.ps1" -BotToken $botToken -OutFile "$Root\miniapp\.env.development.local" | Out-Null
# ⚠️ عمداً به `.env.local` کپی نمی‌شود: Vite آن فایل را در همه حالت‌ها
# می‌خواند، از جمله build — یعنی مقدار «فقط توسعه» به باندل محصول راه
# پیدا می‌کند. محل درست همان `.env.development.local` است.
Remove-Item "$Root\miniapp\.env.local" -ErrorAction SilentlyContinue
Write-Host "==> Mini App initData تازه نوشته شد"

# وابستگی‌های فرانت در صورت نیاز
if (-not (Test-Path "$Root\miniapp\node_modules")) {
    Write-Host "==> npm ci miniapp..."
    Push-Location "$Root\miniapp"; npm.cmd ci; Pop-Location
}
if (-not (Test-Path "$Root\admin-panel\node_modules")) {
    Write-Host "==> npm ci admin-panel..."
    Push-Location "$Root\admin-panel"; npm.cmd ci; Pop-Location
}

# توقف پروسه‌های قبلی روی پورت‌ها (اختیاری، بی‌صدا)
function Stop-Port([int]$Port) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        try { Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue } catch {}
    }
}
Stop-Port 8080
Stop-Port 5173
Stop-Port 5174
Start-Sleep -Seconds 1

Write-Host "==> Backend (پورت 8080)..." -ForegroundColor Green
$envBlock = @"
Set-Location '$Root'
Get-Content '$Root\.env' | ForEach-Object {
  if (`$_ -match '^\s*#' -or `$_ -match '^\s*`$') { return }
  `$i = `$_.IndexOf('='); if (`$i -lt 1) { return }
  Set-Item -Path ('Env:' + `$_.Substring(0,`$i).Trim()) -Value `$_.Substring(`$i+1).Trim()
}
Set-Location '$Root\backend'
if (Test-Path '$jdk25') { `$env:JAVA_HOME='$jdk25' }
mvn spring-boot:run `"-Dspring-boot.run.profiles=dev`"
"@
Start-Process powershell -ArgumentList @("-NoExit", "-Command", $envBlock)

Write-Host "==> منتظر سلامت Backend..."
$healthy = $false
for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 2
        if ($r.StatusCode -eq 200) { $healthy = $true; break }
    } catch {}
    Write-Host "  ... هنوز بالا نیامده ($i)"
}
if (-not $healthy) {
    Write-Host "Backend به‌موقع بالا نیامد. لاگ پنجره Backend را ببینید." -ForegroundColor Red
    exit 1
}
Write-Host "==> Backend OK" -ForegroundColor Green

Write-Host "==> Mini App (5173)..." -ForegroundColor Green
Start-Process powershell -ArgumentList @(
    "-NoExit", "-Command",
    "Set-Location '$Root\miniapp'; npm.cmd run dev"
)

Write-Host "==> Admin Panel (5174)..." -ForegroundColor Green
Start-Process powershell -ArgumentList @(
    "-NoExit", "-Command",
    "Set-Location '$Root\admin-panel'; npm.cmd run dev"
)

# رمز ادمین توسعه — همان مقداری که بک‌اند می‌بیند.
# ⚠️ این «نمایش» است، نه «راز در کد»: رمز seed توسعه‌ای است و
# `DevBootstrap` با `@Profile("dev")` محدود شده. اگر در `.env` عوضش کرده
# باشید، همان نشان داده می‌شود تا بنر با واقعیت یکی بماند.
$adminPassword = "ChangeMe-Admin-1!"
if (Test-Path ".env") {
    $line = Get-Content ".env" | Where-Object { $_ -match '^APP_DEV_ADMIN_PASSWORD=' } | Select-Object -First 1
    if ($line) {
        $v = ($line -split '=', 2)[1].Trim()
        if ($v) { $adminPassword = $v }
    }
}

Write-Host ""
Write-Host "══════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Mini App:     http://localhost:5173"
Write-Host "  Admin Panel:  http://localhost:5174"
Write-Host "  Swagger:      http://localhost:8080/swagger-ui.html"
Write-Host "  Admin login:  superadmin / $adminPassword"
Write-Host "══════════════════════════════════════════════" -ForegroundColor Cyan
