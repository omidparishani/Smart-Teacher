# Runbook عملیاتی — فاز ۱۲

> چک‌لیست عملیاتی برای سلامت، پشتیبان/بازیابی، Mock، Redis/MinIO اختیاری، و CI.

---

## ۱. سلامت سرویس

| بررسی | فرمان / آدرس | انتظار |
|---|---|---|
| Health | `GET /actuator/health` | `{"status":"UP"}` |
| Info | `GET /actuator/info` | بدون Secret؛ env/java/os خاموش‌اند |
| DB | `docker compose ps` → postgres healthy | healthy |
| لاگ شروع | بدون «Using default password» واقعی در prod | فقط کلید از Env |

```bash
curl -sf http://localhost:8080/actuator/health
curl -sf http://localhost:8080/actuator/info
```

---

## ۲. پروفایل و Mock

| متغیر / کلید | prod | توضیح |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | اجباری |
| `app.mock.eitaa` | `false` | هرگز Mock در prod |
| `app.mock.ai` | `false` | |
| `app.mock.payment` | `false` | |
| `app.redis.enabled` | `true` فقط با Redis واقعی | پیش‌فرض `false` → حافظه‌ای |
| `app.storage.provider` | `LOCAL` یا `MINIO` | MinIO stub بدون سرور هم بالا می‌آید |
| `app.security.totp.enabled` | `true` | اجبار کد 2FA برای SUPER_ADMIN با totp_enabled |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | از Env | بدون پیش‌فرض در `application-prod.yml` |
| `ENCRYPTION_MASTER_KEY` / `JWT_SECRET` | از Env | بدون پیش‌فرض |

---

## ۳. محدودیت نرخ ورود

- مسیر: `POST /api/v1/auth/login`
- پیش‌فرض: ۱۰ درخواست / دقیقه / (IP + username)
- پیکربندی: `app.rate-limit.login.*`
- `app.redis.enabled=false` → `InMemoryRateLimiter`
- `app.redis.enabled=true` → `RedisRateLimiter` (شمارندهٔ Redis-compatible؛ بدون Redis زنده هم stub داخل‌فرآیندی دارد)

---

## ۴. پشتیبان‌گیری و بازیابی (چک‌لیست عملی)

### ۴.۱ پشتیبان روزانهٔ PostgreSQL

```bash
# روی میزبان یا sidecar
export PGHOST=localhost PGPORT=5432 PGUSER=smart_teacher PGDATABASE=smart_teacher
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
pg_dump --format=custom --file="backup-${STAMP}.dump"

# رمزنگاری در حالت سکون (مثال با openssl)
openssl enc -aes-256-cbc -salt -pbkdf2 \
  -in "backup-${STAMP}.dump" \
  -out "backup-${STAMP}.dump.enc"
shred -u "backup-${STAMP}.dump"   # حذف نسخهٔ خام
```

- [ ] پشتیبان روزانهٔ رمزنگاری‌شده در مسیر خارج از دیسک اپ
- [ ] نگهداری حداقل ۷ روز غلتان + یک نسخهٔ ماهانه
- [ ] فایل‌های `APP_STORAGE_PATH` (LocalFileStorage) هم در همان پنجره بکاپ می‌شوند

### ۴.۲ تست بازیابی (حداقل هر فصل روی staging)

```bash
# ۱) دیتابیس خالی staging
createdb smart_teacher_restore_test

# ۲) بازیابی
openssl enc -d -aes-256-cbc -pbkdf2 -in backup-XXXX.dump.enc -out restore.dump
pg_restore --clean --if-exists -d smart_teacher_restore_test restore.dump

# ۳) صحت
psql -d smart_teacher_restore_test -c "select count(*) from flyway_schema_history;"
psql -d smart_teacher_restore_test -c "select count(*) from users;"

# ۴) بالا آوردن اپ موقت با DB_URL بازیابی‌شده و چک /actuator/health
```

- [ ] بازیابی کامل روی staging بدون خطای schema
- [ ] ورود ادمین و یک جریان Mini App دود تست می‌شود
- [ ] زمان RTO/RPO ثبت می‌شود

---

## ۵. Partitioning جداول پرحجم (`ai_usage_records`)

> تبدیل جدول موجود به partitioned در PostgreSQL نیاز به ساخت جدول والد جدید،
> کپی داده، و rename اتمیک دارد و تست‌های فعلی روی جدول ساده validate می‌شوند.
> **در این گام عمداً فقط مستند شده** تا بدون شکستن `ddl-auto=validate` بماند.

پیشنهاد آینده (ماهانه روی `created_at`):

```sql
-- روی پنجره نگهداری، نه در Flyway تست‌محور:
CREATE TABLE ai_usage_records_p (
  LIKE ai_usage_records INCLUDING ALL
) PARTITION BY RANGE (created_at);

CREATE TABLE ai_usage_records_y2026m01
  PARTITION OF ai_usage_records_p
  FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- سپس migrate داده + rename در تراکنش نگهداری
```

تا آن زمان ایندکس‌های V19 کافی‌اند.

---

## ۶. Compose اختیاری

```bash
# فقط postgres + backend (پیش‌فرض)
docker compose up -d

# + stubهای redis/minio
docker compose --profile optional up -d
```

پس از بالا آوردن Redis واقعی: `APP_REDIS_ENABLED=true`.
برای MinIO واقعی: `APP_STORAGE_PROVIDER=MINIO` (فعلاً stub حافظه‌ای تا SDK وصل شود).

---

## ۷. CI

- Workflow: `.github/workflows/ci.yml`
- Backend: JDK 25 + `mvn verify` با Testcontainers (`postgres:17-alpine` — هم‌نسخه با `docker-compose.yml`)
- Mini App / Admin Panel: `typecheck` + `test` + `build`
- gitleaks: job اختیاری با `continue-on-error`
- OWASP Dependency-Check: job اختیاری با `continue-on-error`

---

## ۸. 2FA سوپرادمین

1. ورود اولیه با رمز
2. `POST /api/v1/admin/2fa/setup` → secret + otpauth URL
3. `POST /api/v1/admin/2fa/confirm` با کد ۶ رقمی
4. ورودهای بعدی با فیلد `totpCode` در `/auth/login`

برای تست‌های بدون 2FA: کاربران `totp_enabled=false` می‌مانند؛ یا `app.security.totp.enabled=false`.
