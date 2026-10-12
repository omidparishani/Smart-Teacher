# ─────────────────────────────────────────────────────────────
# Smart Teacher — Production Launch Script (Windows PowerShell)
# ─────────────────────────────────────────────────────────────

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  سامانه هوشمند معلم - استقرار پروداکشن  " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$envFile = Join-Path $PSScriptRoot "..\.env"
if (-not (Test-Path $envFile)) {
    Write-Host "[خطا] فایل .env در ریشه پروژه یافت نشد!" -ForegroundColor Red
    Write-Host "لطفاً ابتدا فایل .env.production.example را به .env کپی و مقادیر واقعی را تنظیم کنید." -ForegroundColor Yellow
    exit 1
}

# بررسی ابزار Docker
try {
    $null = docker --version
} catch {
    Write-Host "[خطا] موتور Docker در سیستم شما یافت نشد یا اجرا نیست." -ForegroundColor Red
    exit 1
}

Write-Host "[۱/۳] بررسی و ساخت ایمیج‌ها..." -ForegroundColor Green
$composeFile = Join-Path $PSScriptRoot "..\docker-compose.prod.yml"

docker compose -f $composeFile --env-file $envFile build

Write-Host "[۲/۳] اجرای کانتینرها در پس‌زمینه..." -ForegroundColor Green
docker compose -f $composeFile --env-file $envFile up -d

Write-Host "[۳/۳] وضعیت سرویس‌های استقراریافته:" -ForegroundColor Green
docker compose -f $composeFile ps

Write-Host "`nاستقرار پروداکشن با موفقیت آغاز شد." -ForegroundColor Cyan
Write-Host "برنامک ایتا: http://localhost/" -ForegroundColor Gray
Write-Host "پنل ادمین:   http://localhost/admin/" -ForegroundColor Gray
Write-Host "وب‌سرویس API: http://localhost/api/" -ForegroundColor Gray
