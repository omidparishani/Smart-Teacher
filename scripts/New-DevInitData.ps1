# ساخت initData امضاشده برای توسعه محلی Mini App
# الگوریتم: docs/eitaa.md — secret = HMAC(key=WebAppData, msg=botToken)
#
# 🔴 توکن پیش‌فرض ندارد — عمدی است.
#
# نسخه پیشین `5768337691:AAG…` را به‌عنوان مقدار پیش‌فرض پارامتر داشت. آن رشته
# <b>بردار تست عمومی</b> مستندات ایتاست (`docs/eitaa.md` بخش ۷.۲) و به‌عنوان
# «راز» لو نرفته — و دقیقاً همین مشکل است: توکنی که همه می‌شناسند، هیچ
# محافظتی نمی‌دهد. اگر یک استقرار با همین پیش‌فرض بالا می‌آمد، هر کسی
# می‌توانست `initData` جعل کند (چون امضا با توکنی زده می‌شود که در GitHub است).
#
# ⚠️ این بردار در `docs/eitaa.md`، `EitaaTestData` و allowlist گلایت‌لیکس
# می‌ماند — چون جهت کلید HMAC را قفل می‌کند. تفاوت اینجاست: آن‌جا یک
# <i>ثابت آزمون</i> است، این‌جا یک <i>مقدار پیش‌فرض اجرا</i>. اولی باید بماند،
# دومی باید برود.
#
# ⚠️ `Mandatory` عمداً استفاده نشده: پارامتر اجباری در PowerShell اگر داده
# نشود، «تعاملی از کاربر می‌پرسد» — و این اسکریپت از داخل اسکریپت دیگر و در
# CI صدا زده می‌شود، جایی که کسی نیست پاسخ دهد. شکست صریح با پیام روشن،
# رفتار درست است.
param(
    [string]$BotToken = "",
    [long]$UserId = 100001,
    [string]$OutFile = ""
)

if (-not $BotToken) {
    # ⚠️ `Write-Host` + `exit` و نه `Write-Error`: فراخوان (`run-dev.ps1`)
    # با `$ErrorActionPreference = "Stop"` اجرا می‌شود و `Write-Error` آنجا
    # به یک استثنای زشت تبدیل می‌شود، نه یک پیام خوانا.
    Write-Host "توکن ربات داده نشد. آن را در .env ریشه پروژه بگذارید یا با -BotToken بدهید." -ForegroundColor Red
    exit 1
}

function Get-HmacSha256([byte[]]$Key, [byte[]]$Message) {
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($Key)
    try { return $hmac.ComputeHash($Message) }
    finally { $hmac.Dispose() }
}

function ConvertTo-Hex([byte[]]$Bytes) {
    -join ($Bytes | ForEach-Object { $_.ToString("x2") })
}

$utf8 = [System.Text.Encoding]::UTF8
$authDate = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$userJson = "{`"id`":$UserId,`"first_name`":`"Ali`",`"last_name`":`"Dev`",`"language_code`":`"fa`",`"allows_write_to_pm`":true}"
$userEnc = [Uri]::EscapeDataString($userJson)

$params = [ordered]@{
    auth_date = "$authDate"
    chat_type = "private"
    query_id  = "AAF$UserId"
    user      = $userEnc
}

$dataCheck = ($params.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "`n"
$secret = Get-HmacSha256 ($utf8.GetBytes("WebAppData")) ($utf8.GetBytes($BotToken))
$hash = ConvertTo-Hex (Get-HmacSha256 $secret ($utf8.GetBytes($dataCheck)))

$parts = @()
foreach ($k in $params.Keys) { $parts += "$k=$($params[$k])" }
$parts += "hash=$hash"
$initData = $parts -join "&"

if ($OutFile) {
    $dir = Split-Path -Parent $OutFile
    if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

    $content = @"
VITE_API_URL=http://localhost:8080/api/v1
VITE_DEV_ALLOW_BROWSER=true
VITE_DEV_INIT_DATA=$initData
"@

    # 🔴 بدون BOM نوشته می‌شود — و این عمدی است.
    #
    # `Set-Content -Encoding UTF8` در Windows PowerShell 5.1 یک BOM
    # (EF BB BF) در ابتدای فایل می‌گذارد. dotenv این BOM را حذف نمی‌کند،
    # پس کلید خط اول به "﻿VITE_API_URL" تبدیل می‌شود، با پیشوند
    # `VITE_` تطبیق نمی‌کند و بی‌صدا دور انداخته می‌شود.
    #
    # نتیجه‌اش یک باگ واقعی بود: `VITE_API_URL` خوانده نمی‌شد و برنامک
    # به `/api/v1` نسبی برمی‌گشت — یعنی به خودِ سرور Vite و ۴۰۴، نه به
    # بک‌اند روی ۸۰۸۰. چون متغیرهای بعدی BOM ندارند، فقط خط اول قربانی
    # می‌شد و علت هم نامرئی بود.
    #
    # `UTF8Encoding($false)` در هر دو PowerShell 5.1 و 7 یکسان کار می‌کند.
    [System.IO.File]::WriteAllText(
        $OutFile,
        $content,
        (New-Object System.Text.UTF8Encoding($false))
    )
}

Write-Output $initData
