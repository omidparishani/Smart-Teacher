# قرارداد API

> بخش ۵۷ و ۵۸ خواسته‌ها · نسخه 1.0

---

## ۱. اصول

| اصل | تصمیم |
|---|---|
| نسخه‌بندی | `/api/v1/...` — مسیر، نه Header |
| قالب | JSON (`application/json; charset=UTF-8`) |
| نام‌گذاری فیلد | `camelCase` در JSON، `snake_case` در دیتابیس |
| تاریخ | ISO-8601 با منطقه زمانی (`2026-09-28T10:15:30Z`) |
| شناسه | UUID به‌صورت String |
| صفحه‌بندی | `Pageable` اسپرینگ (`page`, `size`, `sort`) |
| مستندسازی | springdoc-openapi روی `/swagger-ui.html` |
| احراز هویت | `Authorization: Bearer <JWT>` |
| زبان پیام خطا | فارسی |
| زبان کد خطا | انگلیسی (SCREAMING_SNAKE_CASE) |

---

## ۲. ساختار پاسخ موفق

### تک‌منبعی
```json
{
  "id": "0191f2a3-4b5c-7d8e-9f01-234567890abc",
  "fullName": "علی احمدی",
  "createdAt": "2026-09-28T10:15:30Z"
}
```

### فهرست صفحه‌بندی‌شده
```json
{
  "content": [ { "id": "...", "fullName": "..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

> **تصمیم:** از پوشش `{ "data": ... }` استفاده **نمی‌کنیم**. بدنه پاسخ مستقیماً منبع است. دلیل: سادگی و سازگاری با ابزارهای استاندارد. کد وضعیت HTTP حامل معنای موفقیت است.

---

## ۳. ساختار پاسخ خطا (بخش ۵۸)

```json
{
  "timestamp": "2026-09-28T10:15:30Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "داده‌های ورودی نامعتبر است.",
  "traceId": "0191f2a3-4b5c-7d8e-9f01-234567890abc",
  "path": "/api/v1/students",
  "details": [
    { "field": "nationalCode", "message": "کد ملی باید ۱۰ رقم باشد", "rejectedValue": "123" }
  ]
}
```

### کدهای خطای استاندارد

| کد | HTTP | پیام فارسی |
|---|---|---|
| `VALIDATION_FAILED` | 400 | داده‌های ورودی نامعتبر است. |
| `INVALID_REQUEST` | 400 | درخواست نامعتبر است. |
| `UNAUTHENTICATED` | 401 | برای انجام این عملیات باید وارد شوید. |
| `INVALID_CREDENTIALS` | 401 | نام کاربری یا رمز عبور نادرست است. |
| `TOKEN_EXPIRED` | 401 | نشست شما منقضی شده است. دوباره وارد شوید. |
| `TOKEN_REUSE_DETECTED` | 401 | نشست شما به دلایل امنیتی باطل شد. |
| `EITAA_INIT_DATA_INVALID` | 401 | احراز هویت ایتا نامعتبر است. |
| `ACCESS_DENIED` | 403 | شما به این بخش دسترسی ندارید. |
| `FEATURE_NOT_ENTITLED` | 403 | این قابلیت در پلن فعلی شما فعال نیست. |
| `SUBSCRIPTION_EXPIRED` | 403 | اشتراک شما منقضی شده است. |
| `ACCOUNT_DISABLED` | 403 | حساب کاربری شما غیرفعال شده است. |
| `RESOURCE_NOT_FOUND` | 404 | موردی که خواستید یافت نشد. |
| `ENDPOINT_NOT_FOUND` | 404 | آدرس درخواستی یافت نشد. |
| `RESOURCE_CONFLICT` | 409 | این عملیات با وضعیت فعلی سیستم در تناقض است. |
| `DUPLICATE_RESOURCE` | 409 | این مورد قبلاً ثبت شده است. |
| `COUPON_INVALID` | 409 | کد تخفیف نامعتبر است. |
| `USAGE_LIMIT_REACHED` | 429 | سقف استفاده از این قابلیت به پایان رسیده است. |
| `AI_USAGE_LIMIT_REACHED` | 429 | سقف استفاده از هوش مصنوعی این پلن به پایان رسیده است. |
| `RATE_LIMIT_EXCEEDED` | 429 | تعداد درخواست‌های شما بیش از حد مجاز است. |
| `AI_PROVIDER_UNAVAILABLE` | 503 | سرویس هوش مصنوعی موقتاً در دسترس نیست. |
| `AI_STRUCTURED_OUTPUT_FAILED` | 502 | پاسخ هوش مصنوعی قابل پردازش نبود. |
| `PAYMENT_GATEWAY_ERROR` | 502 | خطا در ارتباط با درگاه پرداخت. |
| `INTERNAL_ERROR` | 500 | خطای غیرمنتظره در سیستم رخ داد. |

> **قاعده امنیتی:** پیام `INTERNAL_ERROR` **هرگز** جزئیات استثنا را برنمی‌گرداند. جزئیات فقط در Log با همان `traceId` ثبت می‌شود. کاربر با `traceId` می‌تواند به پشتیبانی مراجعه کند.

---

## ۴. فهرست کامل Endpointها

### ۴.۱ احراز هویت — `/api/v1/auth`

| متد | مسیر | دسترسی | توضیح | وضعیت |
|---|---|---|---|---|
| `POST` | `/auth/eitaa` | عمومی | ورود با `initData` ایتا | ✅ |
| `POST` | `/auth/login` | عمومی | ورود ادمین با نام کاربری/رمز | ✅ فاز ۴ |
| `POST` | `/auth/refresh` | عمومی | تازه‌سازی توکن | ✅ |
| `POST` | `/auth/logout` | کاربر | خروج | ✅ |
| `GET` | `/auth/me` | کاربر | پروفایل جاری | ✅ |
| `POST` | `/auth/switch-tenant` | کاربر | تغییر مستأجر فعال | ⏳ |
| `GET` | `/auth/tenants` | کاربر | مستأجرهای کاربر | ⏳ |

### ۴.۲ کلاس‌ها — `/api/v1/classes`

| متد | مسیر | دسترسی | وضعیت |
|---|---|---|---|
| `GET` | `/classes` | TEACHER | ✅ فاز ۳ |
| `POST` | `/classes` | TEACHER | ✅ فاز ۳ |
| `GET` | `/classes/{id}` | TEACHER | ✅ فاز ۳ |
| `PUT` | `/classes/{id}` | TEACHER | ✅ فاز ۳ |
| `DELETE` | `/classes/{id}` | TEACHER | ✅ فاز ۳ |
| `GET` | `/classes/{id}/students` | TEACHER | ✅ گام ۲ فاز ۳ |
| `POST` | `/classes/{id}/students` | TEACHER | ✅ گام ۲ فاز ۳ |
| `DELETE` | `/classes/{id}/students/{studentId}` | TEACHER | ✅ گام ۲ فاز ۳ |
| `GET` | `/classes/{id}/dashboard` | TEACHER | ✅ گام ۲ فاز ۳ |

> **⚠️ عمق دسترسی این مسیرها از ستون «دسترسی» بیشتر است.** ستون
> `TEACHER` می‌گوید «کارمند مستأجر»؛ ولی معلم فقط کلاس‌هایی را می‌بیند
> که خودش سرکلاسشان است. مالک مستأجر و مدیر مدرسه همه کلاس‌ها را
> می‌بینند. کلاس معلم همکار **۴۰۴** می‌دهد، نه ۴۰۳ — تا کسی با شمردن
> پاسخ‌ها فهرست شناسه‌های معتبر نسازد (تفصیل در `multi-tenancy.md` §۴).

### ۴.۲.۱ درس‌ها — `/api/v1/subjects`

> **⚠️ این بخش در فهرست اولیه Endpointها نبود و افزوده شده است.** ERD
> (`database.md` §۳) جدول `subjects` را الزام کرده و
> `classrooms.subject_id` بدون راهی برای ساختن درس عملاً بی‌استفاده
> می‌ماند. افزودن این مسیرها یک انحراف آگاهانه است، نه فراموشی — در
> `verification-log.md` هم ثبت شده.

| متد | مسیر | دسترسی |
|---|---|---|
| `GET` | `/subjects` | TEACHER |
| `POST` | `/subjects` | TEACHER |
| `GET` | `/subjects/{id}` | TEACHER |
| `PUT` | `/subjects/{id}` | TEACHER |
| `DELETE` | `/subjects/{id}` | TEACHER |

**درس‌ها دارایی کل مستأجرند، نه یک معلم.** برخلاف کلاس‌ها، هر کارمند
مستأجر می‌تواند هر درس را ببیند و ویرایش کند. دلیلش این است که «ریاضی
پایه پنجم» در یک مدرسه یک رکورد است که چند معلم از آن استفاده می‌کنند؛
محدودکردنش به سازنده‌اش، درس را عملاً غیرقابل استفاده می‌کرد.

`DELETE` وقتی **۴۰۹** می‌دهد که کلاسی به درس متصل باشد؛ ابتدا باید
کلاس‌ها از درس جدا شوند. دلیلش: حذف نرم + کلید خارجی
`ON DELETE SET NULL` یعنی قید دیتابیس هرگز فعال نمی‌شود و در صورت
اجازه‌دادن، کلاس‌ها بی‌صدا بی‌نام می‌شدند.

**⚠️ تصمیم باز:** هر کارمند می‌تواند درسِ ساخته‌شده توسط همکارش را
ویرایش یا حذف کند. این با مدل «دارایی مشترک مستأجر» سازگار است و
`created_by` / `updated_by` رکوردش را نگه می‌دارند، ولی اگر روزی
سیاست سخت‌گیرانه‌تری خواستید، نقطه تغییر `SubjectController.STAFF_ONLY`
و `SubjectService.require` است.

### ۴.۳ دانش‌آموزان — `/api/v1/students`

> ✅ **گام ۲ فاز ۳** — CRUD و ثبت‌نام پیاده شده (مهاجرت `V10`).

| متد | مسیر | دسترسی | وضعیت |
|---|---|---|---|
| `GET` | `/students` | TEACHER | ✅ |
| `POST` | `/students` | TEACHER | ✅ |
| `GET` | `/students/{id}` | TEACHER | ✅ — `STUDENT(خود)` بعداً |
| `PUT` | `/students/{id}` | TEACHER | ✅ |
| `DELETE` | `/students/{id}` | TEACHER | ✅ |
| `GET` | `/students/{id}/performance` | TEACHER | ⏳ فاز ۷/۸ |
| `GET` | `/students/{id}/submissions` | TEACHER / STUDENT(خود) | ⏳ فاز ۷ |

> **⚠️ دانش‌آموز دارایی مدرسه است، نه معلم.** برخلاف کلاس‌ها، هر کارمند
> مستأجر همه دانش‌آموزان مدرسه را می‌بیند. قید پذیرش
> `teacherA_cannotReadTeacherB_students` درباره **کلاس** معلم ب است
> (مسیرهای `/classes/{id}/students`)، نه پرونده دانش‌آموز.
>
> **داشبورد کلاس** فقط خلاصه کلاس و شمار دانش‌آموزان فعال را برمی‌گرداند.
> شمار تکالیف و نمرات عمداً نیست — آن‌ها فازهای ۷ و ۸ می‌آورند و صفر
> برگرداندن یعنی «چیزی نیست»، که دروغ است.

### ۴.۴ کتابخانه — `/api/v1/books`

| متد | مسیر | دسترسی | توضیح |
|---|---|---|---|
| `GET` | `/books` | TEACHER | ✅ |
| `POST` | `/books` | TEACHER | ✅ ایجاد کتاب (JSON یا multipart+PDF) |
| `GET` | `/books/{id}` | TEACHER | ✅ |
| `PUT` | `/books/{id}` | TEACHER | ✅ |
| `DELETE` | `/books/{id}` | TEACHER | ✅ |
| `POST` | `/books/{id}/pages` | TEACHER | ✅ بارگذاری صفحه/PDF (چندبخشی) |
| `GET` | `/books/{id}/pages` | TEACHER | ✅ |
| `GET` | `/books/{id}/pages/{pageNumber}` | TEACHER | ✅ |
| `POST` | `/books/{id}/pdf` | TEACHER | ✅ استخراج صفحات PDF |
| `GET` | `/books/{id}/chapters` | TEACHER | ✅ |
| `POST` | `/books/{id}/chapters` | TEACHER | ✅ |
| `POST` | `/books/{id}/pages/{pageNumber}/ocr` | TEACHER | ✅ اجرای OCR (Mock) |
| `POST` | `/books/{id}/pages/{pageNumber}/analyze` | TEACHER | ✅ تحلیل AI |
| `POST` | `/books/{id}/pages/{pageNumber}/questions` | TEACHER | ✅ تولید سؤال |
| `POST` | `/books/{id}/pages/{pageNumber}/teaching-plan` | TEACHER | ✅ طرح درس |
| `GET` | `/books/{id}/analyses` | TEACHER | ✅ تحلیل‌های ذخیره‌شده |

### ۴.۵ تکالیف — `/api/v1/assignments`

> ✅ **فاز ۷ گام ۲** — تصحیح AI + بازبینی Append-Only + reevaluate-all.
> ⚠️ مسیر ارسال پاسخ: `/submissions` (نه `/submit`) — پاسخ از چت ایتا
> دریافت نمی‌شود (`docs/eitaa.md` بخش ۶).

| متد | مسیر | دسترسی | توضیح |
|---|---|---|---|
| `GET` | `/assignments` | TEACHER | ✅ گام ۱ |
| `POST` | `/assignments` | TEACHER | ✅ گام ۱ |
| `GET` | `/assignments/{id}` | TEACHER / STUDENT | ✅ گام ۱ |
| `PUT` | `/assignments/{id}` | TEACHER | ✅ گام ۱ |
| `DELETE` | `/assignments/{id}` | TEACHER | ✅ گام ۱ — soft delete |
| `POST` | `/assignments/{id}/publish` | TEACHER | ✅ گام ۱ — شکست ایتا rollback نمی‌کند |
| `GET` | `/assignments/{id}/submissions` | TEACHER | ✅ گام ۱ |
| `POST` | `/assignments/{id}/submissions` | TEACHER / STUDENT | ✅ گام ۱ — متن؛ مسیر Mini App |
| `POST` | `/assignments/{id}/submissions/{sid}/evaluate` | TEACHER | ✅ گام ۲ |
| `GET` | `/assignments/{id}/submissions/{sid}/evaluation` | TEACHER / STUDENT(خود) | ✅ گام ۲ |
| `POST` | `/assignments/{id}/submissions/{sid}/review` | TEACHER | ✅ گام ۲ — Append-Only |
| `GET` | `/assignments/{id}/submissions/{sid}/reviews` | TEACHER | ✅ گام ۲ |
| `GET` | `/assignments/submissions/{id}` | TEACHER / STUDENT(خود) | ✅ گام ۱ |
| `POST` | `/assignments/{id}/reevaluate-all` | TEACHER | ✅ گام ۲ |

### ۴.۶ آزمون‌ها — `/api/v1/exams`

> ✅ **فاز ۸ گام ۲** — تولید AI (`POST /exams/generate`)، blueprint (`V24`)،
> تصحیح Mock برای SHORT/ESSAY، مهلت `durationMinutes`، نتایج/کارنامه،
> بازنویسی نمره معلم. مهاجرت `V16` + `V24`.

| متد | مسیر | دسترسی | وضعیت |
|---|---|---|---|
| `GET` | `/exams` | TEACHER | ✅ فاز ۸ گام ۱ |
| `POST` | `/exams` | TEACHER | ✅ |
| `GET` | `/exams/{id}` | TEACHER | ✅ |
| `PUT` | `/exams/{id}` | TEACHER | ✅ |
| `DELETE` | `/exams/{id}` | TEACHER | ✅ |
| `POST` | `/exams/generate` | TEACHER | ✅ گام ۲ — Mock AI · MCQ + essay |
| `POST` | `/exams/{id}/questions` | TEACHER | ✅ |
| `PUT` | `/exams/{id}/questions/{qid}` | TEACHER | ✅ |
| `DELETE` | `/exams/{id}/questions/{qid}` | TEACHER | ✅ |
| `POST` | `/exams/{id}/publish` | TEACHER | ✅ |
| `GET` | `/exams/{id}/attempts` | TEACHER | ✅ |
| `GET` | `/exams/{id}/results` | TEACHER | ✅ گام ۲ |
| `POST` | `/exams/{id}/start` | STUDENT | ✅ |
| `POST` | `/exams/attempts/{aid}/answers` | STUDENT | ✅ |
| `POST` | `/exams/attempts/{aid}/submit` | STUDENT | ✅ — MCQ + AI SHORT/ESSAY · رد پس از مهلت |
| `GET` | `/exams/attempts/{aid}` | STUDENT | ✅ |
| `GET` | `/exams/attempts/{aid}/report` | TEACHER / STUDENT | ✅ گام ۲ |
| `POST` | `/exams/{id}/attempts/{aid}/grade` | TEACHER | ✅ گام ۲ — بازنویسی نمره |

### ۴.۷ دستیار هوشمند — `/api/v1/ai`

> ✅ **فاز ۵** — `POST /ai/chat` با `PromptResolver` + `ProviderBroker` + Mock
> (بدون Adapter واقعی Provider).
> (`app.mock.ai=true`). Adapter واقعی Provider هنوز نیست و حدس زده
> نمی‌شود. مسیرهای تاریخچه/تحلیل/بازخورد هنوز بازند.

| متد | مسیر | دسترسی | وضعیت |
|---|---|---|---|
| `POST` | `/ai/chat` | TEACHER | ✅ فاز ۵ — Prompt + Broker + Mock + ثبت مصرف |
| `GET` | `/ai/conversations` | TEACHER | ⏳ |
| `GET` | `/ai/conversations/{id}` | TEACHER | ⏳ |
| `DELETE` | `/ai/conversations/{id}` | TEACHER | ⏳ |
| `POST` | `/ai/analyze-student/{studentId}` | TEACHER | ⏳ |
| `POST` | `/ai/generate-feedback` | TEACHER | ⏳ |
| `POST` | `/ai/generate-class-message` | TEACHER | ⏳ |
| `GET` | `/ai/usage` | TEACHER | ⏳ |

### ۴.۸ گروه‌ها و کانال‌ها — `/api/v1/eitaa`

> ✅ **فاز ۹ گام ۲** — کانال، جاب زمان‌بندی PENDING→SENT، قواعد خودکارسازی
> روی رویدادهای داخلی، `pin` / `viewCountForDelete` در API. مهاجرت `V17` + `V25`.

| متد | مسیر | دسترسی | وضعیت |
|---|---|---|---|
| `GET` | `/eitaa/health` | TEACHER | ✅ — `getMe` ربات |
| `GET` | `/eitaa/accounts` | TEACHER | ⏳ |
| `POST` | `/eitaa/accounts` | TEACHER | ⏳ |
| `POST` | `/eitaa/accounts/{id}/verify` | TEACHER | ⏳ |
| `GET` | `/eitaa/groups` | TEACHER | ✅ فاز ۹ گام ۱ |
| `POST` | `/eitaa/groups` | TEACHER | ✅ |
| `PUT` | `/eitaa/groups/{id}` | TEACHER | ✅ گام A2 — ویرایش جزئی |
| `DELETE` | `/eitaa/groups/{id}` | TEACHER | ✅ گام A2 — حذف نرم |
| `POST` | `/eitaa/groups/{id}/connect-class` | TEACHER | ✅ |
| `DELETE` | `/eitaa/groups/{id}/connect-class` | TEACHER | ✅ |
| `POST` | `/eitaa/groups/{id}/verify` | TEACHER | ✅ — توکن + قالب `chat_id` |
| `POST` | `/eitaa/groups/{id}/send-message` | TEACHER | ✅ — Mock · pin · viewCountForDelete |
| `POST` | `/eitaa/groups/{id}/send-assignment` | TEACHER | ⏳ |
| `GET` | `/eitaa/channels` | TEACHER | ✅ گام ۲ |
| `POST` | `/eitaa/channels` | TEACHER | ✅ گام ۲ |
| `PUT` | `/eitaa/channels/{id}` | TEACHER | ✅ گام A2 — ویرایش جزئی |
| `DELETE` | `/eitaa/channels/{id}` | TEACHER | ✅ گام A2 — حذف نرم |
| `POST` | `/eitaa/channels/{id}/verify` | TEACHER | ✅ — توکن + قالب `chat_id` |
| `POST` | `/eitaa/channels/{id}/send-message` | TEACHER | ✅ گام ۲ |
| `POST` | `/eitaa/channels/{id}/publish-queue` | TEACHER | ✅ — صف انتشار |
| `POST` | `/eitaa/channels/{id}/generate-content` | TEACHER | ⏳ |
| `POST` | `/eitaa/channels/{id}/publish` | TEACHER | ⏳ |
| `GET` | `/eitaa/message-credits` | TEACHER | ✅ — مانده اعتبار پیام |
| `GET` | `/eitaa/messages` | TEACHER | ⏳ |
| `GET` | `/eitaa/scheduled-messages` | TEACHER | ⏳ |
| `POST` | `/eitaa/scheduled-messages` | TEACHER | ✅ — PENDING + جاب + `date` |
| `DELETE` | `/eitaa/scheduled-messages/{id}` | TEACHER | ⏳ |
| `GET` | `/eitaa/automation-rules` | TEACHER | ✅ گام ۲ |
| `POST` | `/eitaa/automation-rules` | TEACHER | ✅ گام ۲ |
| `PUT` | `/eitaa/automation-rules/{id}` | TEACHER | ✅ گام ۲ |
| `DELETE` | `/eitaa/automation-rules/{id}` | TEACHER | ✅ گام ۲ |

> **گام A2 — قرارداد ویرایش جزئی (`PUT`):** `null` یعنی «تغییر نده».
> برای `title` (و `username` در کانال) رشتهٔ خالی یعنی «پاک کن».
> برای `type` رشتهٔ خالی هم یعنی «تغییر نده» — چون ستون `type` در پایگاه
> داده `NOT NULL` است و `null` کردنش استثنای محدودیت می‌دهد.
> `chat_id` فقط وقتی اعتبارسنجی و جایگزین می‌شود که **غیرخالی و متفاوت**
> باشد؛ بدون شرط «متفاوت»، ویرایشِ فقط‌عنوان روی رکوردهای قدیمی بی‌دلیل
> شکست می‌خورد.
>
> **قالب `chat_id` عمداً سخت‌گیر نیست.** سند رسمی ایتا مجموعه‌نویسهٔ مجاز را
> مشخص نمی‌کند، پس هر `regex` دقیقی «حدس» است. فقط چهار چیز رد می‌شود که
> قطعاً غلط‌اند: خالی · بلندتر از ۱۰۰ نویسه · فاصلهٔ داخلی · پیشوند
> `http://` یا `https://`. شناسه **هرگز بازنویسی نمی‌شود** (حذف `@` می‌تواند
> شناسهٔ معتبر را نامعتبر کند). جزئیات: [`docs/eitaa-setup.md`](eitaa-setup.md).


> ❌ **`POST /eitaa/webhook/{botId}` حذف شد.** ایتا هیچ مسیر دریافت پیامی ندارد —
> نه Webhook، نه Polling ([`docs/eitaa.md`](eitaa.md) بخش ۶).
>
> **این یک بهبود امنیتی است:** هیچ Endpoint عمومی بدون احراز هویت وجود ندارد،
> پس سطح حمله جعل درخواست و Replay حذف می‌شود.
> تست `ArchUnit_noWebhookControllerTest` این را تضمین می‌کند.

> 📌 **`POST /eitaa/accounts`** دو نوع حساب می‌پذیرد (`kind: BOT | APP`) و توکن را
> **رمزنگاری‌شده** ذخیره می‌کند. در پاسخ فقط `tokenHint` (۴ کاراکتر آخر) برمی‌گردد —
> **هرگز خود توکن** ([`docs/security.md`](security.md)).

### ۴.۹ اشتراک و پرداخت — `/api/v1/subscription` و `/api/v1/payments`

> ✅ **فاز ۱۰ گام ۱** — پلن‌های عمومی، checkout با `MockPaymentGateway`،
> `EntitlementService` stub. مهاجرت `V18`. درگاه واقعی حدس زده نمی‌شود.

| متد | مسیر | دسترسی | وضعیت |
|---|---|---|---|
| `GET` | `/plans` | عمومی | ✅ فاز ۱۰ گام ۱ |
| `GET` | `/subscription/current` | TEACHER | ✅ |
| `POST` | `/subscription/checkout` | TEACHER | ✅ — Mock |
| `POST` | `/subscription/cancel` | TEACHER | ⏳ |
| `POST` | `/subscription/validate-coupon` | TEACHER | ⏳ |
| `GET` | `/payments` | TEACHER | ⏳ |
| `GET` | `/payments/{id}` | TEACHER | ⏳ |
| `GET` | `/payments/callback/{gateway}` | عمومی | ⏳ |
| `GET` | `/invoices` | TEACHER | ⏳ |
| `GET` | `/invoices/{id}` | TEACHER | ⏳ |
| `GET` | `/wallet` | TEACHER | ⏳ |
| `GET` | `/wallet/transactions` | TEACHER | ⏳ |

### ۴.۱۰ پنل مدیریت — `/api/v1/admin`

> ✅ **فاز ۴ گام ۱–۳** — ورود ادمین، داشبورد شمارشی، مدیریت کاربران
> (CRUD + وضعیت + نقش + رمز)، مدیریت مستأجرها، و Feature Flags حداقلی
> پیاده شده‌اند. صفر جعلی برای مسیرهای باز برنمی‌گردد.

| متد | مسیر | دسترسی | وضعیت |
|---|---|---|---|
| `GET` | `/admin/dashboard` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ — فقط شمارنده‌های واقعی |
| `GET` | `/admin/dashboard/charts?period=30d` | SUPER_ADMIN, ADMIN | ✅ فاز ۱۱ گام ۱ — daily users/tenants/subscriptions |
| `GET` | `/admin/users` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ |
| `POST` | `/admin/users` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ — ساخت ستاد با رمز + نقش |
| `GET` | `/admin/users/{id}` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ |
| `PUT` | `/admin/users/{id}` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ — پروفایل |
| `DELETE` | `/admin/users/{id}` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ — soft delete + audit؛ حذف خود ممنوع |
| `POST` | `/admin/users/{id}/disable` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ |
| `POST` | `/admin/users/{id}/enable` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ |
| `POST` | `/admin/users/{id}/suspend` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ — بدنه: `{ "reason" }` |
| `POST` | `/admin/users/{id}/reset-password` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ — بدنه: `{ "newPassword" }` |
| `POST` | `/admin/users/{id}/change-role` | SUPER_ADMIN, ADMIN | ✅ فاز ۴ — بدنه: `{ "role" }`؛ demote خود ممنوع |
| `POST` | `/admin/users/{id}/change-plan` | SUPER_ADMIN, ADMIN | ⏳ فاز صورتحساب |

| گروه | مسیرها | وضعیت |
|---|---|---|
| مستأجرها | `GET\|POST /admin/tenants`, `GET\|PUT /admin/tenants/{id}` | ✅ فاز ۴ |
| مدارس | `GET\|POST /admin/schools`, `GET\|PUT /admin/schools/{id}` | ✅ فاز ۴ — نگاشت روی `tenants` با `type=SCHOOL` |
| پلن‌ها | `GET\|POST /admin/plans`, … | ✅ فاز ۱۰ گام ۱ |
| امکانات | `GET /admin/features`, `PUT /admin/features/{code}`, `POST /admin/features/overrides` | ✅ فاز ۴ — V14 |
| اشتراک‌ها | `GET /admin/subscriptions`, … | ⏳ |
| پرداخت‌ها | `GET /admin/payments`, … | ⏳ |
| کوپن‌ها | `GET\|POST /admin/coupons`, … | ⏳ |
| Provider / Model | `GET\|POST /admin/ai/providers`, `POST /admin/ai/providers/{id}/test`, `GET /admin/ai/models` | ✅ فاز ۵ — فقط `tokenHint`؛ تست Mock OK |
| Prompt | `GET\|POST /admin/prompts`, `…/versions`, `…/publish`, `…/rollback`, `…/test` | ✅ فاز ۵ — CRUD + Playground Mock |
| CSRF | `GET /admin/csrf` | ✅ فاز ۴ — توکن برای SPA؛ کوکی HttpOnly ورود هنوز باقی است |
| مصرف و گزارش | `GET /admin/reports/users`, `/subscriptions`, `/ai`, `/revenue`, `/users.csv` | ✅ فاز ۱۱ گام ۱ |
| تیکت‌ها | `/admin/support/tickets` | ⏳ |
| حسابرسی | `GET /admin/audit-logs` | ⏳ |
| تنظیمات / سلامت | `/admin/settings`, `/admin/system/...` | ⏳ |

---

## ۵. نمونه درخواست/پاسخ

### ورود با ایتا

```http
POST /api/v1/auth/eitaa
Content-Type: application/json

{
  "initData": "user=%7B%22id%22%3A123...&auth_date=1759000000&hash=abc..."
}
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "0191f2a3-...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": {
    "id": "0191f2a3-...",
    "fullName": "علی احمدی",
    "roles": ["TEACHER"]
  },
  "tenant": {
    "id": "0191f2b4-...",
    "name": "کلاس‌های علی احمدی",
    "type": "TEACHER"
  },
  "subscription": {
    "planCode": "FREE",
    "planName": "رایگان",
    "status": "ACTIVE",
    "endDate": "2026-10-28T00:00:00Z"
  },
  "isNewUser": false
}
```

### ایجاد تکلیف

```http
POST /api/v1/assignments
Authorization: Bearer <token>
Content-Type: application/json

{
  "classroomId": "0191f2c5-...",
  "title": "تمرین فصل سوم",
  "description": "صفحه‌های ۴۵ تا ۴۷ را حل کنید.",
  "type": "HOMEWORK",
  "maxScore": 20,
  "dueAt": "2026-10-05T18:00:00Z",
  "bookId": "0191f2d6-...",
  "pageFrom": 45,
  "pageTo": 47,
  "aiEvaluationEnabled": true,
  "publishImmediately": true
}
```

```json
{
  "id": "0191f2e7-...",
  "title": "تمرین فصل سوم",
  "status": "PUBLISHED",
  "maxScore": 20,
  "dueAt": "2026-10-05T18:00:00Z",
  "classroom": { "id": "0191f2c5-...", "name": "پنجم الف" },
  "submissionCount": 0,
  "eitaaMessageStatus": "SENT",
  "createdAt": "2026-09-28T10:15:30Z"
}
```

### ارزیابی تکلیف با AI

```http
POST /api/v1/assignments/submissions/0191f2f8-.../evaluate
Authorization: Bearer <token>
```

```json
{
  "submissionId": "0191f2f8-...",
  "aiEvaluation": {
    "score": 17.5,
    "correct": true,
    "mistakes": [
      "در سؤال ۳، یکای اندازه‌گیری نوشته نشده است."
    ],
    "strengths": [
      "روش حل سؤال ۱ کاملاً درست است.",
      "محاسبات سؤال ۲ بدون خطا انجام شده."
    ],
    "weaknesses": [
      "دقت در نوشتن یکاها"
    ],
    "feedback": "کار خوبی انجام دادی علی! فقط یادت باشه همیشه یکای اندازه‌گیری رو بنویسی.",
    "teacherNote": "مفهوم را فهمیده، فقط در نوشتن یکاها بی‌دقت است. در کلاس تأکید کنم.",
    "confidence": 0.92
  },
  "finalScore": null,
  "requiresTeacherReview": false,
  "reviewReason": null,
  "usage": {
    "inputTokens": 512,
    "outputTokens": 187,
    "estimatedCost": 0.0042,
    "provider": "anthropic",
    "model": "claude-sonnet-5",
    "durationMs": 2103
  }
}
```

> **نکته:** `finalScore` تا زمانی که معلم بازبینی نکند `null` است. `requiresTeacherReview` وقتی `true` می‌شود که `confidence < 0.7` باشد — یعنی AI خودش می‌داند که مطمئن نیست.

### خطای سقف مصرف

```http
POST /api/v1/ai/chat
```

```json
{
  "timestamp": "2026-09-28T10:15:30Z",
  "status": 429,
  "code": "AI_USAGE_LIMIT_REACHED",
  "message": "سقف استفاده از هوش مصنوعی این پلن به پایان رسیده است.",
  "traceId": "0191f2a3-...",
  "path": "/api/v1/ai/chat",
  "details": {
    "metric": "AI_REQUESTS",
    "limit": 50,
    "used": 50,
    "resetsAt": "2026-10-28T00:00:00Z",
    "suggestedPlanCode": "PRO",
    "walletBalance": 0
  }
}
```

---

## ۶. OpenAPI / Swagger

- مسیر UI: `/swagger-ui.html`
- مسیر JSON: `/v3/api-docs`
- گروه‌بندی: `Mini App API`، `Admin API`
- در `prod`: **غیرفعال** یا پشت احراز هویت (خطر نشت ساختار داخلی)

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartTeacherOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("دستیار هوشمند معلم — API")
                .version("v1")
                .description("API پلتفرم آموزشی دستیار هوشمند معلم"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

---

## ۷. صفحه‌بندی، فیلتر و مرتب‌سازی

```http
GET /api/v1/students?page=0&size=20&sort=fullName,asc&search=علی&status=ACTIVE
```

| پارامتر | توضیح |
|---|---|
| `page` | از صفر |
| `size` | حداکثر ۱۰۰ (بالاتر → کلمپ، ضد DoS) |
| `sort` | `field,direction` — فقط فیلدهای مجاز |
| `search` | جست‌وجوی متنی (`pg_trgm` برای فارسی) |

> **نکته امنیتی:** فیلدهای مجاز مرتب‌سازی **Allow-List** هستند. اگر فیلد دلخواه پذیرفته شود، مهاجم می‌تواند با مرتب‌سازی روی ستون‌های ایندکس‌نشده، دیتابیس را تحت فشار بگذارد.

---

## ۸. Idempotency

برای عملیات مالی، Header اختیاری:

```http
POST /api/v1/subscription/checkout
Idempotency-Key: 0191f2a3-4b5c-7d8e-9f01-234567890abc
```

اگر همان کلید دوباره ارسال شود، پاسخ قبلی برگردانده می‌شود (بدون ساخت پرداخت جدید).

---

## ۹. قالب پیام‌های فارسی

تمام پیام‌های خطای کاربرپسند فارسی هستند. اما **کد خطا انگلیسی** است تا:
- Frontend بتواند بر اساس کد رفتار متفاوت داشته باشد
- Log و Monitoring قابل جست‌وجو باشد
- تست‌ها به متن فارسی وابسته نشوند

```java
public enum ErrorCode {
    VALIDATION_FAILED(400, "داده‌های ورودی نامعتبر است."),
    AI_USAGE_LIMIT_REACHED(429, "سقف استفاده از هوش مصنوعی این پلن به پایان رسیده است."),
    // ...
    ;

    private final int status;
    private final String defaultMessage;
}
```
