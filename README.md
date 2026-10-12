# دستیار هوشمند معلم

> **Smart Teacher** — پلتفرم SaaS آموزشی برای معلمان، مدارس و مؤسسات آموزشی

سامانه‌ای مبتنی بر ربات و Mini App ایتا که فعالیت‌های روزمره معلم را مدیریت می‌کند: کلاس‌ها، دانش‌آموزان، کتاب‌ها، تکالیف، آزمون‌ها و دستیار هوش مصنوعی.

---

## وضعیت پروژه

**فاز فعلی:** فازهای ۱–۱۲ از نظر کد بسته شده‌اند. Backend: `Tests run: 511` (آخرین `mvn -B clean verify` روی درخت قفل‌شده — `Failures: 0, Errors: 0`). گام A (بحرانی: ماندگاری ورود، سلامت ایتا، ریت‌لیمیت تمام مسیرهای احراز هویت، ردیس ری‌پلی‌گارد) ✅ · گام B (سیستم طراحی پنل ادمین، سایدبار تیره، نمودارها، CRUD کامل تمام موجودیت‌ها، لاگ حسابرسی و تنظیمات) ✅ · گام D (کانفیگ استقرار پروداکشن، Nginx امن با رعایت iframe ایتا، داکر کمپوز پروداکشن و چک‌لیست‌ها) ✅.

| فاز | عنوان | وضعیت |
|---|---|---|
| ۰ | طراحی معماری و مستندسازی | ✅ تأییدشده |
| ۱ | زیرساخت | ✅ |
| ۲ | یکپارچه‌سازی ایتا | ✅ · اتصال، سلامت و مدیریت کامل |
| ۳ | دامنه معلم | ✅ کامل |
| ۴ | پنل مدیریت پیشرفته | ✅ · سایدبار تیره + نمودارها + CRUD کامل تمام موجودیت‌ها + لاگ حسابرسی + تنظیمات |
| ۵ | هوش مصنوعی | ✅ Mock/Live · CB · compare · resolve-trace · مدیریت مدل‌ها و پرووایدرها |
| ۶ | کتابخانه | ✅ |
| ۷ | تکالیف | ✅ · deep-link UUID · usage concurrency |
| ۸ | آزمون | ✅ · generate/manage/results Mini App · blueprint · publish |
| ۹ | کنترلر ایتا | ✅ · automation · credits · channels/rules/schedule UI |
| ۱۰ | تجاری | ✅ Mock · coupons/wallet · Admin plans/coupons · expiry warning |
| ۱۱ | فروش و گزارش | ✅ · AI-by-user · revenue series · Excel/charts |
| ۱۲ | Production | 🔄 stubs · 2FA · Redis RL · CB/cache · k6 · observability · بدون pen-test/Deploy |

شرح کامل: [`docs/roadmap.md`](docs/roadmap.md)

---

## معماری در یک نگاه

```text
                    ┌────────────────────┐
                    │   دستیار هوشمند    │
                    │       معلم         │
                    └─────────┬──────────┘
                              │
                ┌─────────────┴─────────────┐
                │                           │
          Mini App ایتا               پنل مدیریت
          (معلم — موبایل)            (Super Admin — وب)
                │                           │
                └─────────────┬─────────────┘
                              │
                    ┌─────────▼─────────┐
                    │  Backend          │
                    │  Modular Monolith │
                    │  Java 25 / SB 4.1 │
                    └─────────┬─────────┘
                              │
        ┌─────────┬───────────┼───────────┬─────────┐
        │         │           │           │         │
   PostgreSQL   Eitaa      AI Providers  Payment  Storage
   (Multi-      (فقط خروجی) Anthropic    Gateways (Local/
    Tenant)      Bot: گروه  xAI/OpenAI   Zarinpal  MinIO)
                 App: کاربر Gemini/...
```

> 🔴 **مهم:** راستی‌آزمایی مستندات رسمی ایتا نشان داد **Bot API تنها ۳ متد دارد و هیچ مسیر دریافت پیامی وجود ندارد.**
> ربات ایتا **فقط می‌فرستد، نمی‌شنود**. Mini App ایتا هم **فقط روی اندروید و ویندوز** اجرا می‌شود.
> جزئیات کامل و اثرهای معماری: [`docs/verification-log.md`](docs/verification-log.md)

---

## پشته فناوری

| لایه | فناوری | نسخه |
|---|---|---|
| زبان | Java | **25 (LTS)** |
| فریم‌ورک | Spring Boot | **4.1.1** |
| ماژول‌بندی | Spring Modulith | 2.1.1 |
| پایگاه داده | PostgreSQL | 17 |
| Migration | Flyway | 13.8.0 |
| مستندسازی API | springdoc-openapi | 3.1.1 |
| تست | JUnit 5 + Testcontainers + ArchUnit | 1.21.4 / 1.5.1 |
| بیلد | Maven | 3.9.14 |
| Mini App | React + TypeScript + Vite | 19 |
| پنل مدیریت | React + TypeScript + Vite | 19 |
| استقرار | Docker Compose | |

---

## اصول حاکم

| # | اصل |
|---|---|
| ۱ | **هیچ چیزی Hardcode نیست** — Provider، Model، Prompt، پلن، قیمت و محدودیت‌ها داده‌اند |
| ۲ | **جداسازی مستأجر در دو لایه** — `@TenantId` هایبرنیت + `TenantGuard` سرویس |
| ۳ | **Fail-Closed** — در نبود مستأجر، سیستم خطا می‌دهد نه اینکه داده لو بدهد |
| ۴ | **حدس ممنوع** — هر API خارجی از مستندات رسمی تأیید می‌شود، وگرنه Adapter + Mock · ثبت در [`docs/verification-log.md`](docs/verification-log.md) |
| ۵ | **Feature کامل** — Entity + Repository + Service + DTO + Controller + تست + مستندات |
| ۶ | **Secret هرگز خام نمی‌ماند** — AES-256-GCM، بدون نشت در Log/DTO/Swagger |
| ۷ | **ویرایش انسانی بر AI مقدم است** — نمره AI پیشنهاد است، نه حکم |

---

## مستندات

### معماری
| سند | محتوا |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | معماری کلان، ماژول‌ها، نمودارها، اهداف غیرکارکردی |
| [`docs/multi-tenancy.md`](docs/multi-tenancy.md) | استراتژی جداسازی مستأجر |
| [`docs/database.md`](docs/database.md) | ERD کامل و طراحی جداول |
| [`docs/security.md`](docs/security.md) | مدل تهدید، احراز هویت، رمزنگاری، RBAC |

### دامنه
| سند | محتوا |
|---|---|
| [`docs/ai-providers.md`](docs/ai-providers.md) | معماری Provider، رزولوشن، Failover، هزینه |
| [`docs/prompt-system.md`](docs/prompt-system.md) | سیستم پرامپت، نسخه‌بندی، Playground |
| [`docs/billing.md`](docs/billing.md) | پلن، اشتراک، Entitlement، مصرف، کیف پول |
| [`docs/payments.md`](docs/payments.md) | معماری درگاه پرداخت |

### یکپارچه‌سازی
| سند | محتوا |
|---|---|
| [`docs/eitaa.md`](docs/eitaa.md) | Bot API **فقط خروجی**، Mini App SDK، اعتبارسنجی initData |
| [`docs/eitaa-setup.md`](docs/eitaa-setup.md) | **راه‌اندازی اتصال واقعی به کانال و گروه — گام‌به‌گام** |
| [`docs/api.md`](docs/api.md) | قرارداد API و کدهای خطا |

### رابط کاربری
| سند | محتوا |
|---|---|
| [`docs/mini-app.md`](docs/mini-app.md) | معماری Mini App و راهنمای توسعه |
| [`docs/admin-panel.md`](docs/admin-panel.md) | معماری پنل مدیریت |

### عملیات
| سند | محتوا |
|---|---|
| [`docs/development.md`](docs/development.md) | راه‌اندازی محیط توسعه |
| [`docs/roadmap.md`](docs/roadmap.md) | نقشه راه فازها |
| [`docs/verification-log.md`](docs/verification-log.md) | **ثبت راستی‌آزمایی** — چه چیزی تأییدشده و چه چیزی حدس |

---

## راه‌اندازی سریع

```bash
# پیش‌نیاز: JDK 25، Maven 3.9+، Node 22+، Docker
# ⚠️ JAVA_HOME سیستم ممکن است روی JDK 21 باشد — راهنمای Toolchain در docs/development.md

cp .env.example .env          # مقادیر را پر کنید
bash scripts/generate-keys.sh # تولید کلیدهای امنیتی

docker compose up -d postgres
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Mini App

```bash
cd miniapp
npm ci
npm run dev                   # http://localhost:5173
```

> ⚠️ Mini App **بیرون از ایتا** به‌طور پیش‌فرض صفحه «این برنامه باید از داخل ایتا باز شود»
> را نشان می‌دهد — این رفتار درست است، نه خطا. برای کار روی دستگاه توسعه،
> متغیر `VITE_DEV_ALLOW_BROWSER=true` این گارد را **فقط در حالت DEV** باز می‌کند.
>
> 🔴 ساده‌ترین راه، اسکریپت آماده است که همه‌ی این‌ها را با هم انجام می‌دهد
> (Postgres، `initData` تازه، متغیرهای محیطی، هر سه سرویس):
>
> ```powershell
> powershell -ExecutionPolicy Bypass -File scripts\run-dev.ps1
> ```
>
> اگر دستی بالا می‌آورید، سه چیز باید با هم درست باشند:
>
> | مورد | مقدار درست | چرا |
> |---|---|---|
> | `VITE_DEV_ALLOW_BROWSER` | `true` در `.env.development.local` | بدونش گارد پلتفرم **پیش از** هر تماس شبکه‌ای می‌بندد |
> | `VITE_DEV_INIT_DATA` | تازه (اعتبار ۵ دقیقه) | `scripts\New-DevInitData.ps1` می‌سازد؛ کهنه → «نشست ایتا منقضی شده است» |
> | `app.cors.allowed-origins` | `http://localhost:5173` | پروفایل `dev` دارد؛ بدونش مرورگر با `ERR_FAILED` جلوی درخواست را می‌گیرد |
>
> ⚠️ مقدار `VITE_DEV_INIT_DATA` را در **`.env.development.local`** بگذارید، نه
> `.env.local` — Vite فایل `.env.local` را در بیلد Production هم می‌خواند.
> جزئیات: [`docs/mini-app.md`](docs/mini-app.md) بخش‌های ۴.۵–۴.۷.

```bash
npm run build                 # tsc -b + vite build + verify-bundle
npm run test                  # Vitest
npm run e2e                   # Playwright — نیازمند Chrome نصب‌شده روی سیستم
npm run lint                  # ESLint
npm run size-check            # بودجه باندل (۱۵۰ kB gzip)
```

> ⚠️ **`npm run e2e` به Chrome نصب‌شده روی سیستم وصل می‌شود** (`channel: 'chrome'`)،
> چون دانلود خودکار Chromium روی این شبکه با خطای گواهی TLS شکست می‌خورد. روی ماشینی
> که Chrome ندارد، این آزمون‌ها اجرا نمی‌شوند — جزئیات: بخش ۱۲.۵ Verification Log.

جزئیات کامل: [`docs/development.md`](docs/development.md)

---

## حالت Mock

سیستم **بدون هیچ API واقعی** قابل اجرا و تست است:

```yaml
app:
  mock:
    eitaa: true      # ربات ایتا شبیه‌سازی می‌شود
    ai: true         # پاسخ‌های AI قطعی و تکرارپذیر
    payment: true    # درگاه پرداخت جعلی با تأیید خودکار
```

---

## ساختار مخزن

```text
smart-teacher/
├── backend/          # Spring Boot — Modular Monolith
├── miniapp/          # Mini App ایتا — React + Vite
├── admin-panel/      # پنل مدیریت — React + Vite
├── docs/             # مستندات
├── docker/           # پیکربندی Prometheus (پیکربندی Nginx هنوز ساخته نشده — فاز ۱۲)
├── scripts/          # اسکریپت‌های کمکی
├── .github/          # CI/CD
├── docker-compose.yml
└── README.md
```

---

## امنیت

اگر آسیب‌پذیری‌ای یافتید، **Issue عمومی باز نکنید.** به آدرس امنیتی پروژه گزارش دهید.

نکات امنیتی پیاده‌سازی‌شده:

- رمزنگاری کلیدهای API با AES-256-GCM
- جداسازی کامل مستأجر با دو لایه دفاعی
- اعتبارسنجی `initData` ایتا با مقایسه زمان-ثابت
- چرخش Refresh Token با تشخیص استفاده مجدد
- محدودیت نرخ **خروجی** روی تماس‌های API ایتا (`EitaaRateLimiter`)
- لاگ بدون داده حساس (ماسک‌گذاری در سطح Logback)

> ⚠️ **محدودیت نرخ روی مسیرهای ورودی** (`/auth/eitaa`, `/auth/refresh`, `/auth/logout`)
> **هنوز پیاده نشده** و در فاز ۱۲ با Redis انجام می‌شود. عمداً اینجا نیامده — چیزی که
> پیاده نشده نباید در فهرست «پیاده‌شده‌ها» بنشیند.

جزئیات: [`docs/security.md`](docs/security.md)

---

## مجوز

مشخص نشده — در انتظار تصمیم مالک محصول.
