# مستند استقرار در محیط عملیاتی (Production Deployment Guide)

این سند راهنمای کامل استقرار و اجرای سامانهٔ **Smart Teacher** در سرورهای ابری و عملیاتی بر اساس معماری چندمستأجره (Multi-Tenant)، پایگاه‌داده امن، کش توزیع‌شده ردیس، فیلترهای محدودیت نرخ، و اتصال مطمئن به پیام‌رسان ایتا است.

---

## ۱. معماری استقرار پروداکشن

استقرار پروداکشن بر مبنای کانفیگ `docker-compose.prod.yml` و یک پروکسی معکوس Nginx پیکربندی شده است:

```
                          [ کاربر / پیام‌رسان ایتا ]
                                      │
                                  (پورت ۸۰/۴۴۳)
                                      ▼
                        ┌───────────────────────────┐
                        │       Nginx Proxy         │
                        └─────────────┬─────────────┘
                                      │
             ┌────────────────────────┼────────────────────────┐
             ▼                        ▼                        ▼
     ┌───────────────┐        ┌───────────────┐        ┌───────────────┐
     │    miniapp    │        │  admin-panel  │        │    backend    │
     │  (SPA / ایتا) │        │ (مدیریت کل)   │        │  (Spring Boot)│
     └───────────────┘        └───────────────┘        └───────┬───────┘
                                                               │
                                              ┌────────────────┴────────────────┐
                                              ▼                                 ▼
                                      ┌───────────────┐                 ┌───────────────┐
                                      │   PostgreSQL  │                 │     Redis     │
                                      │ (بدون پورت باز)│                 │ (بدون پورت باز)│
                                      └───────────────┘                 └───────────────┘
```

### اصول امنیتی شبکه:
1. هیچ پورتی از پایگاه داده (PostgreSQL) یا کش (Redis) به شبکه عمومی هاست متصل نمی‌شود (`No published ports`).
2. دسترسی به APIهای بک‌اند فقط از طریق روت `/api/` و پشت فیلترهای Nginx و محدودیت نرخ انجام می‌شود.
3. **تله حیاتی iframe ایتا:**
   - مینی‌اپلیکیشن معلم داخل WebView و iframe کلاینت‌های وب ایتا لود می‌شود؛ از این رو هدر CSP آن با `frame-ancestors 'self' https://eitaa.com https://web.eitaa.com https://*.eitaa.com;` تنظیم شده و هرگز `X-Frame-Options: DENY` ندارد.
   - پنل ادمین (`/admin/`) برعکس، دارای `X-Frame-Options: DENY` و `frame-ancestors 'none'` است تا در برابر هرگونه سوءاستفاده یا Clickjacking محافظت شود.

---

## ۲. پیش‌نیازهای سرور
- سیستم‌عامل لینوکس (Ubuntu 22.04 LTS / 24.04 LTS یا Debian 12 توصیه می‌شود)
- موتور Docker (نسخه ۲۴ به بالا) و افزونه Docker Compose v2
- دامنه یا زیردامنه با گواهی SSL معتبر (برای پروتکل HTTPS مینی‌اپ ایتا الزامی است)
- حداقل منابع سخت‌افزاری: ۴ هسته پردازنده، ۸ گیگابایت رم، ۳۰ گیگابایت حافظه NVMe

---

## ۳. مراحل راه‌اندازی و اجرای پروداکشن

### گام اول: کپی متغیرهای محیطی
```bash
cp .env.production.example .env
```
مقادیر زیر را حتماً با کلیدهای قدرتمند و امن در فایل `.env` پر کنید:
- `DB_PASSWORD`: گذرواژه قدرتمند و یکتای دیتابیس
- `REDIS_PASSWORD`: گذرواژه اتصال به ردیس
- `JWT_SECRET`: کلید رمزنگاری توکن‌ها (حداقل ۶۴ کاراکتر رندوم)
- `ENCRYPTION_MASTER_KEY`: کلید رمزنگاری داده‌های حساس در پایگاه داده
- `APP_EITAA_BOT_TOKEN`: توکن معتبر ربات ایتا دریافت شده از بات‌فادر
- `APP_EITAA_APP_TOKEN`: کلید مینی‌اپلیکیشن ایتا دریافت شده از پرتال توسعه‌دهندگان
- `APP_CORS_ALLOWED_ORIGINS`: آدرس دامنهٔ پروداکشن سامانه

### گام دوم: بیلد و اجرای کانتینرها
در لینوکس:
```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```
یا در سرور ویندوز با اسکریپت آماده:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-prod.ps1
```

### گام سوم: راستی‌آزمایی سرویس‌ها
```bash
# بررسی وضعیت کانتینرها
docker compose -f docker-compose.prod.yml ps

# بررسی وضعیت سلامت بک‌اند
curl -I http://localhost/actuator/health
```

---

## ۴. مدیریت و پایش پروداکشن

- **لاگ‌های بک‌اند:**
  ```bash
  docker compose -f docker-compose.prod.yml logs -f backend
  ```
- **لاگ‌های دسترسی Nginx:**
  ```bash
  docker compose -f docker-compose.prod.yml logs -f nginx
  ```
- **پشتیبان‌گیری از دیتابیس:**
  ```bash
  docker exec -t st-prod-postgres pg_dump -U smart_teacher smart_teacher | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz
  ```
