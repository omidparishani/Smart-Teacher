# راه‌اندازی محیط توسعه

> نسخه 1.0 · راستی‌آزمایی‌شده در 2026-09-28

---

## ۱. پیش‌نیازها

| ابزار | نسخه | وضعیت در سیستم شما |
|---|---|---|
| JDK | **25.0.2** | ✅ نصب در `C:\Program Files\Java\jdk-25.0.2` |
| Maven | 3.9.14 | ✅ نصب |
| Node.js | 22.18.0 | ✅ نصب |
| Docker | 29.7.2 | ✅ نصب |
| Git | هر نسخه اخیر | — |

---

## ۲. ⚠️ نکته حیاتی: `JAVA_HOME` سیستم روی JDK 21 است

در سیستم شما:

```text
JAVA_HOME = C:\Program Files\Java\jdk-21.0.10
```

ولی پروژه به **JDK 25** نیاز دارد. اگر با تنظیمات فعلی بیلد کنید، Maven از JDK 21 استفاده می‌کند و بیلد شکست می‌خورد.

### راه‌حل: Maven Toolchains

پروژه از **Toolchain** استفاده می‌کند تا بیلد **مستقل از `JAVA_HOME`** همیشه با JDK 25 انجام شود.

**فایل `~/.m2/toolchains.xml`** (در ویندوز: `C:\Users\<نام کاربری>\.m2\toolchains.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0">
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>25</version>
      <vendor>oracle</vendor>
    </provides>
    <configuration>
      <jdkHome>C:\Program Files\Java\jdk-25.0.2</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**در `backend/pom.xml`:**

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-toolchains-plugin</artifactId>
      <version>3.2.0</version>
      <executions>
        <execution>
          <goals><goal>toolchain</goal></goals>
        </execution>
      </executions>
      <configuration>
        <toolchains>
          <jdk>
            <version>25</version>
          </jdk>
        </toolchains>
      </configuration>
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <release>25</release>
        <compilerArgs>
          <arg>-parameters</arg>
        </compilerArgs>
      </configuration>
    </plugin>
  </plugins>
</build>
```

**صفت‌های پروژه:**

```xml
<properties>
  <java.version>25</java.version>
  <maven.compiler.release>25</maven.compiler.release>
</properties>
```

### تأیید درست بودن تنظیم

```bash
cd backend
mvn -version
# باید نشان دهد: Java version: 25.0.2

mvn help:evaluate -Dexpression=java.version -q -DforceStdout
# باید نشان دهد: 25
```

اگر Maven همچنان JDK 21 نشان داد، یعنی Toolchain پیدا نشده — مسیر `jdkHome` در `toolchains.xml` را بررسی کنید.

### راه‌حل جایگزین (موقت)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
```

> این راه‌حل فقط برای همان ترمینال است و با بستن آن از بین می‌رود. **Toolchain راه‌حل پایدار است.**

---

## ۳. قیدهای فنی راستی‌آزمایی‌شده

این‌ها در همین محیط کامپایل و تست شده‌اند. **نقض نکنید:**

| قابلیت | وضعیت JDK 25 | استفاده در پروژه |
|---|---|---|
| `ScopedValue` | ✅ **نهایی** | `TenantContext` — نگهداری مستأجر جاری |
| Virtual Threads | ✅ نهایی | `Executors.newVirtualThreadPerTaskExecutor()` |
| Record Patterns | ✅ نهایی | نگاشت DTO |
| Pattern Matching for switch | ✅ نهایی | `switch` روی نوع رویداد |
| **`StructuredTaskScope`** | ⚠️ **هنوز Preview** | ❌ **استفاده نمی‌شود** — به‌جایش Virtual Thread + Future |

> **هشدار:** استفاده از `StructuredTaskScope` بدون `--enable-preview` باعث **شکست بیلد** می‌شود. ما از آن استفاده نمی‌کنیم. اگر در آینده لازم شد، نیاز به فعال‌سازی صریح Preview دارد که برای کد Production توصیه نمی‌شود.

---

## ۴. راه‌اندازی سریع

```bash
# ۱. کلون
git clone <repository-url> smart-teacher
cd smart-teacher

# ۲. متغیرهای محیطی
cp .env.example .env
# مقادیر را پر کنید — راهنمای تولید کلید در بخش ۶

# ۳. پایگاه داده
docker compose up -d postgres

# ۴. بک‌اند
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# ۵. Mini App (ترمینال جدید)
cd miniapp
npm install
npm run dev

# ۶. پنل مدیریت (ترمینال جدید)
cd admin-panel
npm install
npm run dev
```

| سرویس | آدرس |
|---|---|
| Backend | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| سلامت | http://localhost:8080/actuator/health |
| Mini App | http://localhost:5173 |
| Admin Panel | http://localhost:5174 |

---

## ۵. متغیرهای محیطی

فایل `.env.example` (در Git، بدون مقدار واقعی):

```bash
# ─── پایگاه داده ───
DB_URL=jdbc:postgresql://localhost:5432/smart_teacher
DB_USERNAME=smart_teacher
DB_PASSWORD=changeme

# ─── امنیت ───
# تولید: openssl rand -base64 32
ENCRYPTION_MASTER_KEY=
# تولید: openssl rand -base64 48
JWT_SECRET=

# ─── ایتا ───
# 🔴 نام canonical — در dev و prod هر دو کار می‌کند (relaxed binding).
# صورت‌های کوتاه EITAA_BOT_TOKEN / EITAA_APP_TOKEN هم پذیرفته می‌شوند،
# ولی فقط چون صریحاً در application-dev.yml و application-prod.yml نگاشت
# شده‌اند. اگر شک کردید، نام canonical را بگذارید.
# توکن ربات ایتایار — ارسال به گروه/کانال (https://eitaayar.ir)
APP_EITAA_BOT_TOKEN=
# توکن برنامک — ارسال به کاربر (developer.eitaa.com)
APP_EITAA_APP_TOKEN=
# اعتبارسنجی از راه دور ایتا — پیش‌فرض خاموش
# (HMAC محلی کافی است؛ سرویس ایتا محدودیت ۲۰ req/s دارد)
APP_EITAA_REMOTE_VERIFY_ENABLED=false

# ─── حالت Mock (توسعه) ───
APP_MOCK_EITAA=true
APP_MOCK_AI=true
APP_MOCK_PAYMENT=true

# ─── CORS (الزامی برای پروداکشن) ───
# پیش‌فرض خالی = هیچ مبدأیی مجاز نیست. چند مبدأ را با کاما جدا کنید.
APP_CORS_ALLOWED_ORIGINS=

# ─── Redis (اختیاری) ───
APP_REDIS_ENABLED=false

# ─── ذخیره‌سازی فایل ───
APP_STORAGE_PROVIDER=LOCAL
APP_STORAGE_PATH=./data/files

# ─── Mini App ───
VITE_API_URL=http://localhost:8080/api/v1
VITE_DEV_INIT_DATA=
```

> **قاعده:** `.env` در `.gitignore` است. `.env.example` هرگز مقدار واقعی ندارد.

---

## ۶. تولید کلیدها

**اسکریپت `scripts/generate-keys.sh`:**

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "ENCRYPTION_MASTER_KEY=$(openssl rand -base64 32)"
echo "JWT_SECRET=$(openssl rand -base64 48)"
```

اجرا:

```bash
bash scripts/generate-keys.sh
```

**در ویندوز (PowerShell):**

```powershell
$key = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($key)
"ENCRYPTION_MASTER_KEY=$([Convert]::ToBase64String($key))"

$secret = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($secret)
"JWT_SECRET=$([Convert]::ToBase64String($secret))"
```

> ⚠️ **`ENCRYPTION_MASTER_KEY` را گم نکنید.** اگر گم شود، تمام API Keyهای ذخیره‌شده در دیتابیس **قابل بازیابی نیستند** و باید دستی دوباره وارد شوند. این کلید را در جای امن (Vault، Password Manager سازمانی) نگهداری کنید.

---

## ۷. پروفایل‌های Spring

| پروفایل | کاربرد | ویژگی‌ها |
|---|---|---|
| `dev` (پیش‌فرض) | توسعه محلی | Mock فعال، SQL لاگ، Swagger باز |
| `test` | تست خودکار | Testcontainers، Mock کامل |
| `prod` | Production | هیچ مقدار پیش‌فرضی، Swagger بسته، SQL لاگ خاموش |

```yaml
# application.yml — مشترک
app:
  name: smart-teacher
spring:
  application:
    name: smart-teacher
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate      # همیشه validate — هرگز update
    open-in-view: false       # ضد LazyInitializationException در Controller
  flyway:
    enabled: true
    locations: classpath:db/migration
```

```yaml
# application-dev.yml
app:
  mock:
    eitaa: true
    ai: true
    payment: true
spring:
  jpa:
    show-sql: true
logging:
  level:
    ir.omid.smartteacher: DEBUG
```

```yaml
# application-prod.yml — هیچ مقدار پیش‌فرضی
spring:
  jpa:
    show-sql: false
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    env:
      enabled: false          # ⚠️ خطر نشت Secret
    configprops:
      enabled: false          # ⚠️ خطر نشت Secret
```

> **`ddl-auto: validate` هرگز به `update` تغییر نمی‌کند.** تغییر Schema فقط با Flyway. `update` در Production می‌تواند داده را نابود کند.

---

## ۸. پایگاه داده

```bash
# بالا آوردن
docker compose up -d postgres

# اتصال
docker compose exec postgres psql -U smart_teacher -d smart_teacher

# اجرای Migrationها (خودکار هنگام بالا آمدن بک‌اند)
cd backend && mvn flyway:migrate

# وضعیت Migrationها
mvn flyway:info

# بازنشانی کامل دیتابیس توسعه (⚠️ داده پاک می‌شود)
docker compose down -v && docker compose up -d postgres
```

> ⚠️ `docker compose down -v` **همه Volumeها را پاک می‌کند.** فقط در محیط توسعه.

---

## ۹. دستورات پرکاربرد

### Backend

```bash
cd backend

mvn clean verify                     # بیلد + همه تست‌ها
mvn test                             # فقط تست‌ها
mvn test -Dtest=TenantIsolationTest  # یک تست مشخص
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn clean package -DskipTests        # بیلد سریع
mvn dependency:tree                  # درخت وابستگی‌ها
mvn versions:display-dependency-updates
```

> ⚠️ **تست‌های دیتابیسی در این محیط به دو متغیر نیاز دارند.** تصویر پیش‌فرض
> `postgres:17-alpine` است (همان نسخه Production)، ولی اگر Docker Hub در دسترس
> نباشد باید تصویر دیگری داد و همان اجرا باید با همان نسخه در
> `verification-log.md` ثبت شود:
>
> ```bash
> # لینوکس / macOS / Git Bash
> TESTCONTAINERS_RYUK_DISABLED=true mvn test \
>   -Dtest=ClassRoomTenantIsolationTest \
>   -Dtest.postgres.image=postgres:16-alpine
> ```
>
> ```powershell
> # PowerShell
> $env:TESTCONTAINERS_RYUK_DISABLED = "true"
> mvn test -Dtest=ClassRoomTenantIsolationTest -Dtest.postgres.image=postgres:16-alpine
> ```
>
> ⚠️ `TESTCONTAINERS_RYUK_DISABLED=true` لازم است چون Ryuk روی این محیط بالا
> نمی‌آید. عارضه‌اش پیام `Surefire is going to kill self fork JVM` در پایان
> اجراست — **اخطار است، نه شکست بیلد.**
>
> ⚠️ زیر PowerShell مسیر `pom.xml` را با `-f` بدهید؛ اگر `cd` جا به‌جا شده
> باشد، Maven با «there is no POM in this directory» رد می‌شود.

### Frontend

```bash
cd miniapp          # یا admin-panel
npm install
npm run dev
npm run typecheck    # tsc -b
npm run build        # tsc -b + vite build + verify-bundle
npm run test         # Vitest
npm run e2e          # Playwright — فقط miniapp
npm run lint
npm run size-check   # کنترل بودجه باندل
```

> ⚠️ **`npm run e2e` فقط در `miniapp` وجود دارد** و به Chrome نصب‌شده روی سیستم وصل
> می‌شود (`channel: 'chrome'`)، چون دانلود Chromium روی شبکه‌های با پروکسی TLS بازدارنده
> شکست می‌خورد. روی ماشینی که Chrome ندارد، این آزمون‌ها اجرا نمی‌شوند — جزئیات:
> `docs/verification-log.md` بخش ۱۲.۵.

> 🔴 **`npm run build` فقط بیلد نیست — یک کنترل امنیتی هم هست.** زنجیره‌اش
> `tsc -b && vite build && node scripts/verify-bundle.mjs` است و آن اسکریپت آخر، اگر
> مقدار توسعه به باندل رسیده باشد **بیلد را با شکست متوقف می‌کند**. اگر کسی
> `verify-bundle.mjs` را از `package.json` بردارد، آن کنترل خاموش می‌شود.

### Docker

```bash
docker compose up -d                    # همه سرویس‌ها
docker compose logs -f backend          # لاگ بک‌اند
docker compose build --no-cache backend # بیلد مجدد
docker compose down
```

---

## ۱۰. تست

### ساختار

⚠️ **تست‌ها بر اساس نوع جدا نشده‌اند — کنار همان ماژولی‌اند که تست می‌کنند.**
پوشه‌های `unit/`, `integration/`, `architecture/` که پیش‌تر اینجا نوشته شده بود
وجود ندارند. قاعده واقعی: بسته آزمون، آینه بسته تولید است.

```text
backend/src/test/java/ir/omid/smartteacher/
├── ApplicationModulesTest.java        مرز ماژول‌ها (Spring Modulith)
├── ArchitectureTest.java              قواعد ArchUnit
├── classroom/
│   ├── ClassRoomTenantIsolationTest.java   دیتابیسی — Testcontainers
│   └── internal/application/               آزمون خصوصی همان لایه
│       ├── ClassRoomServiceAccessTest.java
│       └── SearchPatternTest.java
├── eitaa/                             ۱۸ فایل آزمون الزامی docs/eitaa.md بخش ۱۳
├── security/                          رمزنگاری، JWT، ماسک داده حساس
├── tenant/                            بستر آزمون جداسازی + TenantContextTest
├── common/                            ابزارهای مشترک (UUIDv7، صفحه‌بندی)
└── ...
```

**تفکیک عملی «unit» و «integration» این است:** هر کلاسی که
`@SpringBootTest` دارد دیتابیسی است و به Docker نیاز دارد؛ بقیه با
Mockito و بدون Docker در چند ثانیه اجرا می‌شوند. جست‌وجوی
`@SpringBootTest` همان فهرست تست‌های کند را می‌دهد.

⚠️ **پسوند فایل مهم است.** هیچ پلاگین Failsafe در `pom.xml` بسته نشده، پس
فقط Surefire اجرا می‌شود و Surefire الگوی `*Test` را می‌خواند. یک فایل با
نام `*IT` **بی‌صدا نادیده گرفته می‌شود** و هیچ خطایی هم نمی‌دهد.

### Testcontainers

تست‌های یکپارچه از PostgreSQL واقعی استفاده می‌کنند (نه H2) — چون:

- `JSONB` در H2 وجود ندارد
- رفتار `@TenantId` هایبرنیت در H2 متفاوت است
- Migrationهای Flyway باید در همان موتور واقعی تست شوند

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TenantIsolationTest {

    // تصویر پارامتری است تا بتوان در محیطی که به Docker Hub دسترسی
    // ندارد، تصویر موجود روی ماشین را داد. پیش‌فرض همان نسخه Production است.
    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    @Test
    void tenantA_cannotReadTenantB_resource() {
        // ...
    }
}
```

> ⚠️ **تست‌ها بدون Docker «رد» نمی‌شوند — شکست می‌خورند.** هیچ
> `@Testcontainers(disabledWithoutDocker = true)` در پروژه نیست. اگر Docker
> بالا نباشد یا تصویر قابل دریافت نباشد، تست با `ContainerFetchException`
> می‌افتد. این عمدی است: تست جداسازی مستأجر قید پذیرش فاز ۱ است و «سبزِ
> بی‌آزمون» بدتر از «قرمزِ صادق» است.

#### ⚠️ دو محدودیت محیطی که اجرای تست را مشروط می‌کنند

اگر `docker pull` با `403 Forbidden` از `production.cloudfront.docker.com`
رد شود (شبکه‌های سازمانی با پروکسی رایج است)، دو چیز لازم می‌شود:

```bash
# ۱) Ryuk (کانتینر جمع‌کننده زباله) هم از Docker Hub می‌آید و همان ۴۰۳ را
#    می‌گیرد. Testcontainers پیش از هر کانتینر دیگری اول آن را می‌خواهد،
#    پس بدون این متغیر هیچ تستی حتی شروع نمی‌شود.
# ۲) تصویر PostgreSQL موجود روی ماشین را بدهید (docker images).
TESTCONTAINERS_RYUK_DISABLED=true mvn -B clean verify \
  -Dtest.postgres.image=postgres:16-alpine
```

> ⚠️ **`TESTCONTAINERS_RYUK_DISABLED` را در `pom.xml` ثابت نکنید.** Ryuk همان
> چیزی است که کانتینرهای تست را پاک می‌کند؛ خاموش‌کردن دائمی‌اش روی CI به
> انباشت کانتینر و پر شدن دیسک می‌انجامد. راه‌حل درست، رفع دسترسی شبکه است.
>
> ⚠️ **و اگر مجبور شدید نسخه‌ای جز ۱۷ بدهید، بدانید نتیجه‌تان مشروط است.**
> سبز بودن روی ۱۶ درباره ۱۷ چیزی ثابت نمی‌کند. هر چنین اجرایی باید در
> [`verification-log.md`](verification-log.md) با همان نسخه ثبت شود.

### اجرای گروهی

```bash
mvn test -Dtest='*Test'                    # همه
mvn test -Dtest='TenantIsolationTest'      # یکی
mvn test -Dgroups=security                 # گروه امنیتی
```

---

## ۱۱. عیب‌یابی

| مشکل | علت | راه‌حل |
|---|---|---|
| `release version 25 not supported` | Maven از JDK 21 استفاده می‌کند | Toolchain را تنظیم کنید (بخش ۲) |
| `No toolchain found for type jdk` | `toolchains.xml` پیدا نشد | فایل را در `~/.m2/` بسازید |
| `StructuredTaskScope is a preview API` | استفاده از API پیش‌نمایشی | Virtual Thread استفاده کنید |
| `Port 5432 already in use` | PostgreSQL دیگری در حال اجراست | پورت را در `docker-compose.yml` عوض کنید |
| `relation "..." does not exist` | Migration اجرا نشده | `mvn flyway:migrate` |
| `Flyway checksum mismatch` | Migration اجراشده ویرایش شده | Migration اجراشده را **هرگز** ویرایش نکنید |
| `TenantNotResolvedException` | درخواست بدون `TenantContext` | ترتیب Filterها را بررسی کنید |
| `LazyInitializationException` | `open-in-view` روشن | در Service با DTO برگردانید، نه Entity |
| `ENCRYPTION_MASTER_KEY باید ۳۲ بایت باشد` | طول کلید اشتباه | `openssl rand -base64 32` |
| Docker در Testcontainers پیدا نمی‌شود | Docker Desktop اجرا نیست | Docker را اجرا کنید |
| `npm run dev` خطای پورت | پورت اشغال | پورت را در `vite.config.ts` عوض کنید |

---

## ۱۲. قواعد کیفیت کد

| قاعده | ابزار | وضعیت در پروژه |
|---|---|---|
| فرمت یکدست | استانداردهای فرمت‌بندی جاوا در IDE | دستی/محیطی (پلاگین spotless در pom.xml نیست) |
| تحلیل ایستا | خطاهای کامپایل با فلگ‌های سخت‌گیرانه | فعال در javac |
| نقض مرز ماژول | `ApplicationModules.verify()` | فعال در تست‌های Modulith |
| قواعد معماری | ArchUnit | فعال در مجموعه تست‌ها |
| Secret در Git | `gitleaks` (pre-commit hook) | پیشنهاد شده در چک‌لیست پروداکشن |
| قرارداد Commit | Conventional Commits | الزامی |

### Git Hooks

```bash
# نصب pre-commit hook
bash scripts/install-hooks.sh
```

که شامل:
1. `spotless:check` روی فایل‌های Java تغییر‌یافته
2. `gitleaks protect --staged`
3. ESLint روی فایل‌های TS تغییر‌یافته

---

## ۱۳. ساختار مخزن

```text
smart-teacher/
├── backend/
│   ├── src/main/java/ir/omid/smartteacher/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-test.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/
│   ├── src/test/java/ir/omid/smartteacher/
│   ├── Dockerfile
│   └── pom.xml
├── miniapp/
│   ├── src/
│   └── package.json
├── admin-panel/
│   ├── src/
│   └── package.json
├── docs/
├── docker/
│   └── prometheus/
├── scripts/
├── .github/workflows/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

> ⚠️ **آنچه در این درخت *نیست* و هنوز ساخته نشده** (گام D برنامه اصلاح ۷ موردی):
>
> | فایل | وضعیت |
> |---|---|
> | `backend/Dockerfile` | ✅ موجود (چندمرحله‌ای، Temurin 25) |
> | `miniapp/Dockerfile` | ❌ ساخته نشده |
> | `admin-panel/Dockerfile` | ❌ ساخته نشده |
> | `docker/nginx/` + `nginx.conf` | ❌ ساخته نشده |
> | `docker-compose.prod.yml` | ❌ ساخته نشده |
> | `.env.production.example` | ❌ ساخته نشده |
>
> این‌ها پیش‌تر در همین سند به‌عنوان «موجود» فهرست شده بودند که **نادرست بود**
> و اصلاح شد. تا ساخته‌شدن‌شان، اجرای پروداکشن با `docker-compose.yml` و
> پروفایل `prod` انجام نمی‌شود.

---

## ۱۴. شروع کار روی یک Feature جدید

چون شما توسعه‌دهنده بک‌اند هستید، مسیر استاندارد:

```text
۱. Migration بنویسید        backend/src/main/resources/db/migration/V<n>__<name>.sql
۲. Entity بسازید             <module>/internal/domain/
۳. Repository بسازید         <module>/internal/domain/
۴. DTO بسازید                <module>/internal/api/dto/
۵. Mapper بسازید (MapStruct) <module>/internal/api/mapper/
۶. Service + ServiceImpl     <module>/internal/application/
۷. Controller                <module>/internal/api/
۸. Exception اختصاصی         common/exception/
۹. تست‌ها                    unit + integration + security
۱۰. مستندسازی Swagger        @Operation روی Controller
۱۱. به‌روزرسانی docs/api.md
```

> **قاعده بخش ۷۲ خواسته‌ها:** تا مرحله ۱۱ تمام نشود، Feature تمام‌شده حساب نمی‌شود.
