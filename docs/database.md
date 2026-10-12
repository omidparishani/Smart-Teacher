# طراحی دیتابیس و ERD

> بخش B سند طراحی · نسخه 1.0 · PostgreSQL 17

---

## ۱. قواعد عمومی طراحی

| قاعده | تصمیم | دلیل |
|---|---|---|
| کلید اصلی | `UUID` نسخه ۷ (زمان‌مرتب) | جلوگیری از حدس ID (ضد-IDOR)، مرتب‌سازی زمانی، ادغام آسان |
| تولید UUID | در Java (`UuidV7Generator`) | Postgres 17 تابع `uuidv7()` ندارد (در ۱۸ اضافه شد)؛ کنترل کامل در اپ |
| زمان | `TIMESTAMPTZ` | هرگز `TIMESTAMP` بدون منطقه زمانی |
| نام‌گذاری | `snake_case`، جدول جمع | یکدستی با SQL |
| پول | `NUMERIC(19,4)` | هرگز `float`/`double` برای پول |
| ارز | جدول `currencies` + کد ISO-4217 | چندارزی در آینده |
| Enum | `VARCHAR(30)` + `CHECK` یا جدول Lookup | `ENUM` بومی Postgres در Migration دردسر دارد |
| حذف | Soft Delete (`deleted_at`) برای موجودیت‌های دامنه | بازیابی و حسابرسی |
| کلیدهای بیرونی | `ON DELETE RESTRICT` پیش‌فرض | حذف آبشاری تصادفی داده آموزشی فاجعه است |
| JSON | `JSONB` | تنظیمات انعطاف‌پذیر (Provider config، Prompt variables) |
| بهینه‌سازی | `optimistic locking` با `version` | جلوگیری از گم‌شدن به‌روزرسانی همزمان |

### ستون‌های استاندارد هر جدول

```sql
id          UUID        PRIMARY KEY,
created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
created_by  UUID        NULL REFERENCES users(id),
updated_by  UUID        NULL REFERENCES users(id),
version     BIGINT      NOT NULL DEFAULT 0
```

و برای جدول‌های **Tenant-Owned**:

```sql
tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT
```

### ایندکس‌های الزامی

```sql
-- روی هر جدول Tenant-Owned
CREATE INDEX idx_<table>_tenant ON <table>(tenant_id);

-- برای کوئری‌های رایج، ایندکس ترکیبی با tenant_id در ابتدا
CREATE INDEX idx_students_tenant_class ON students(tenant_id, classroom_id);
```

> **قاعده حیاتی:** در جدول‌های Tenant-Owned، `tenant_id` **همیشه اولین ستون** ایندکس ترکیبی است. این هم کارایی را بالا می‌برد و هم جداسازی را در سطح Storage تقویت می‌کند.

---

## ۲. ERD — هویت و مستأجر (Identity & Tenancy)

```mermaid
erDiagram
    USERS ||--o{ TENANT_MEMBERS : "عضو"
    TENANTS ||--o{ TENANT_MEMBERS : "دارد"
    TENANTS ||--o{ TENANTS : "والد-فرزند"
    USERS ||--o{ REFRESH_TOKENS : "نشست"
    USERS ||--o{ USER_ROLES : "نقش"
    ROLES ||--o{ USER_ROLES : "دارد"
    ROLES ||--o{ ROLE_PERMISSIONS : "دارد"
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : "دارد"
    USERS ||--o| EITAA_IDENTITIES : "وصل‌شده به ایتا"

    USERS {
        uuid id PK
        varchar username UK "nullable برای کاربر ایتا"
        varchar password_hash "nullable — فقط ادمین/معلم وب"
        varchar full_name
        varchar email UK "nullable"
        varchar mobile UK "nullable"
        varchar status "ACTIVE|DISABLED|SUSPENDED|PENDING"
        varchar preferred_locale "fa"
        varchar theme "LIGHT|DARK|SYSTEM"
        timestamptz last_login_at
        timestamptz deleted_at
    }

    TENANTS {
        uuid id PK
        varchar type "TEACHER|SCHOOL|ORGANIZATION"
        varchar name
        varchar slug UK
        uuid parent_tenant_id FK
        uuid owner_user_id FK
        varchar status "ACTIVE|SUSPENDED|ARCHIVED"
        jsonb settings
        timestamptz deleted_at
    }

    TENANT_MEMBERS {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        varchar role "OWNER|SCHOOL_ADMIN|TEACHER|ASSISTANT"
        varchar status
        timestamptz joined_at
    }

    EITAA_IDENTITIES {
        uuid id PK
        uuid user_id FK
        bigint eitaa_user_id UK
        varchar eitaa_username
        varchar first_name
        varchar last_name
        varchar language_code
        boolean is_premium
        timestamptz linked_at
    }

    ROLES {
        uuid id PK
        varchar code UK "SUPER_ADMIN|ADMIN|TEACHER|SCHOOL_ADMIN|STUDENT"
        varchar name
        boolean is_system "نقش سیستمی غیرقابل حذف"
    }

    PERMISSIONS {
        uuid id PK
        varchar code UK "CLASS_CREATE|AI_CONFIG_WRITE|..."
        varchar resource
        varchar action
    }
```

**نکات:**
- `users` Platform-Owned است — `tenant_id` ندارد. یک کاربر می‌تواند عضو چند مستأجر باشد.
- `eitaa_identities` جداست تا `users` آلوده به جزئیات ایتا نشود و اتصال چند پیام‌رسان (فاز آینده) راحت باشد.
- `roles` و `permissions` جدولی‌اند تا نقش سفارشی در آینده بدون تغییر کد اضافه شود.

---

## ۳. ERD — آموزش (Teaching Domain)

```mermaid
erDiagram
    TENANTS ||--o{ CLASSROOMS : "دارد"
    CLASSROOMS ||--o{ CLASS_STUDENTS : "ثبت‌نام"
    STUDENTS ||--o{ CLASS_STUDENTS : "عضو"
    CLASSROOMS ||--o{ CLASS_TEACHERS : "معلم"
    USERS ||--o{ CLASS_TEACHERS : "تدریس"
    TENANTS ||--o{ STUDENTS : "دارد"
    TENANTS ||--o{ SUBJECTS : "دارد"
    SUBJECTS ||--o{ CLASSROOMS : "موضوع"

    CLASSROOMS {
        uuid id PK
        uuid tenant_id FK
        varchar name "مثلاً پنجم الف"
        varchar grade_level "پنجم"
        varchar academic_year "1404-1405"
        uuid subject_id FK
        uuid homeroom_teacher_id FK
        integer capacity
        varchar status "ACTIVE|ARCHIVED"
        timestamptz deleted_at
    }

    STUDENTS {
        uuid id PK
        uuid tenant_id FK
        varchar full_name
        varchar national_code UK "nullable"
        varchar guardian_mobile
        date birth_date
        varchar gender
        varchar status "ACTIVE|INACTIVE|GRADUATED"
        jsonb metadata
        timestamptz deleted_at
    }

    CLASS_STUDENTS {
        uuid id PK
        uuid tenant_id FK
        uuid classroom_id FK
        uuid student_id FK
        varchar roll_number "شماره حضور"
        timestamptz enrolled_at
    }

    CLASS_TEACHERS {
        uuid id PK
        uuid tenant_id FK
        uuid classroom_id FK
        uuid user_id FK
        varchar role "HOMEROOM|SUBJECT|ASSISTANT"
    }

    SUBJECTS {
        uuid id PK
        uuid tenant_id FK
        varchar name "ریاضی"
        varchar code
        varchar grade_level
    }
```

---

## ۴. ERD — کتاب و محتوا (Content)

```mermaid
erDiagram
    TENANTS ||--o{ BOOKS : "دارد"
    BOOKS ||--o{ BOOK_PAGES : "دارد"
    BOOKS ||--o{ BOOK_CHAPTERS : "دارد"
    BOOK_CHAPTERS ||--o{ BOOK_PAGES : "شامل"
    BOOKS ||--o{ CONTENT_ANALYSES : "تحلیل"
    BOOK_PAGES ||--o{ CONTENT_ANALYSES : "تحلیل"

    BOOKS {
        uuid id PK
        uuid tenant_id FK
        varchar title "ریاضی پنجم دبستان"
        varchar publisher
        varchar grade_level
        uuid subject_id FK
        integer total_pages
        varchar status "DRAFT|PROCESSING|READY|FAILED"
        uuid cover_file_id FK
        timestamptz deleted_at
    }

    BOOK_CHAPTERS {
        uuid id PK
        uuid tenant_id FK
        uuid book_id FK
        varchar title
        integer order_index
        integer start_page
        integer end_page
    }

    BOOK_PAGES {
        uuid id PK
        uuid tenant_id FK
        uuid book_id FK
        integer page_number
        uuid file_id FK "تصویر/PDF صفحه"
        text extracted_text "خروجی OCR"
        varchar ocr_status "PENDING|DONE|FAILED|SKIPPED"
        numeric ocr_confidence
        jsonb layout_metadata
    }

    CONTENT_ANALYSES {
        uuid id PK
        uuid tenant_id FK
        varchar target_type "BOOK|PAGE|CHAPTER"
        uuid target_id
        varchar analysis_type "SUMMARY|QUESTIONS|TEACHING_PLAN|DIFFICULTY"
        text result_text
        jsonb result_json
        uuid ai_usage_id FK
        uuid prompt_version_id FK
        timestamptz deleted_at
    }
```

> **تصمیم طراحی:** `CONTENT_ANALYSES` به‌صورت **چندریختی (polymorphic)** طراحی شده (`target_type` + `target_id`) تا در فاز بعد بتوان تحلیل «فصل» یا «کل کتاب» را بدون جدول جدید اضافه کرد. هزینه: عدم امکان FK واقعی — با `CHECK` و تست پوشش داده می‌شود.

---

## ۵. ERD — تکلیف (Assignment)

```mermaid
erDiagram
    CLASSROOMS ||--o{ ASSIGNMENTS : "دارد"
    ASSIGNMENTS ||--o{ ASSIGNMENT_SUBMISSIONS : "ارسال‌ها"
    STUDENTS ||--o{ ASSIGNMENT_SUBMISSIONS : "ارسال"
    ASSIGNMENT_SUBMISSIONS ||--o| AI_EVALUATIONS : "ارزیابی AI"
    ASSIGNMENT_SUBMISSIONS ||--o{ TEACHER_REVIEWS : "بازبینی معلم"
    ASSIGNMENTS ||--o{ ASSIGNMENT_ATTACHMENTS : "پیوست"
    FILES ||--o{ ASSIGNMENT_ATTACHMENTS : ""
    FILES ||--o{ ASSIGNMENT_SUBMISSION_FILES : ""

    ASSIGNMENTS {
        uuid id PK
        uuid tenant_id FK
        uuid classroom_id FK
        uuid book_id FK "nullable"
        integer page_from
        integer page_to
        varchar title
        text description
        varchar type "HOMEWORK|PROJECT|PRACTICE|QUIZ"
        numeric max_score
        timestamptz due_at
        varchar status "DRAFT|PUBLISHED|CLOSED|ARCHIVED"
        boolean ai_evaluation_enabled
        uuid prompt_version_id FK "nullable — پرامپت اختصاصی"
        uuid created_by FK
        timestamptz deleted_at
    }

    ASSIGNMENT_SUBMISSIONS {
        uuid id PK
        uuid tenant_id FK
        uuid assignment_id FK
        uuid student_id FK
        text answer_text
        varchar status "PENDING|SUBMITTED|AI_EVALUATED|REVIEWED|GRADED|RETURNED"
        timestamptz submitted_at
        numeric final_score "نمره نهایی (پس از بازبینی)"
        boolean teacher_overrode_ai
    }

    AI_EVALUATIONS {
        uuid id PK
        uuid tenant_id FK
        uuid submission_id FK
        numeric ai_score
        boolean is_correct
        jsonb mistakes
        jsonb strengths
        jsonb weaknesses
        text feedback
        text teacher_note
        numeric confidence "0..1"
        uuid ai_usage_id FK
        uuid prompt_version_id FK
        varchar status "SUCCESS|PARTIAL|FAILED|LOW_CONFIDENCE"
    }

    TEACHER_REVIEWS {
        uuid id PK
        uuid tenant_id FK
        uuid submission_id FK
        uuid reviewer_user_id FK
        varchar action "ACCEPT|EDIT_SCORE|EDIT_FEEDBACK|REJECT|RE_EVALUATE"
        numeric previous_score
        numeric new_score
        text comment
        timestamptz reviewed_at
    }
```

> **نکته کلیدی (بخش ۴۰ خواسته‌ها — Human Review):** `TEACHER_REVIEWS` یک **لاگ Append-Only** است. هیچ‌وقت رکورد قبلی overwrite نمی‌شود. `ASSIGNMENT_SUBMISSIONS.final_score` تنها منبع حقیقت نهایی است و `AI_EVALUATIONS.ai_score` فقط پیشنهاد است.

---

## ۶. ERD — آزمون (Exam)

```mermaid
erDiagram
    CLASSROOMS ||--o{ EXAMS : "دارد"
    EXAMS ||--o{ EXAM_QUESTIONS : "دارد"
    EXAMS ||--o{ EXAM_ATTEMPTS : "تلاش‌ها"
    STUDENTS ||--o{ EXAM_ATTEMPTS : "شرکت"
    EXAM_ATTEMPTS ||--o{ EXAM_ANSWERS : "پاسخ‌ها"
    EXAM_QUESTIONS ||--o{ EXAM_ANSWERS : "پاسخ"
    EXAMS ||--o{ EXAM_BLUEPRINTS : "طرح تولید"

    EXAMS {
        uuid id PK
        uuid tenant_id FK
        uuid classroom_id FK
        uuid book_id FK "nullable"
        varchar title
        text description
        integer duration_minutes
        numeric total_score
        timestamptz start_at
        timestamptz end_at
        varchar status "DRAFT|SCHEDULED|ACTIVE|FINISHED|ARCHIVED"
        boolean shuffle_questions
        boolean show_results_immediately
        uuid prompt_version_id FK
        timestamptz deleted_at
    }

    EXAM_BLUEPRINTS {
        uuid id PK
        uuid tenant_id FK
        uuid exam_id FK
        uuid book_id FK
        integer page_from
        integer page_to
        jsonb difficulty_distribution "EASY:40,MEDIUM:40,HARD:20"
        jsonb question_type_distribution "MCQ:10,SHORT:5,ESSAY:2"
        integer question_count
        uuid ai_usage_id FK
    }

    EXAM_QUESTIONS {
        uuid id PK
        uuid tenant_id FK
        uuid exam_id FK
        varchar type "MCQ|TRUE_FALSE|SHORT_ANSWER|ESSAY|FILL_BLANK"
        text question_text
        jsonb options "برای MCQ"
        text correct_answer
        numeric score
        varchar difficulty "EASY|MEDIUM|HARD"
        text explanation
        integer order_index
        boolean ai_generated
        uuid book_page_id FK "nullable — منبع"
    }

    EXAM_ATTEMPTS {
        uuid id PK
        uuid tenant_id FK
        uuid exam_id FK
        uuid student_id FK
        timestamptz started_at
        timestamptz submitted_at
        numeric score "nullable تا تصحیح"
        varchar status "IN_PROGRESS|SUBMITTED|GRADING|GRADED|ABSENT"
        integer attempt_number
    }

    EXAM_ANSWERS {
        uuid id PK
        uuid tenant_id FK
        uuid attempt_id FK
        uuid question_id FK
        text answer_text
        jsonb selected_options
        numeric awarded_score
        boolean is_correct
        boolean ai_graded
        text ai_feedback
        uuid ai_usage_id FK
    }
```

---

## ۷. ERD — ایتا (Eitaa Integration)

> ⚠️ **این بخش پس از راستی‌آزمایی مستندات رسمی ایتا بازنویسی شد** ([`docs/eitaa.md`](eitaa.md) نسخه ۲.۰).
> چون **ایتا هیچ مسیر دریافت پیام ندارد**، جدول `MESSAGE_INBOUND` و فیلدهای Webhook **حذف شدند**.

```mermaid
erDiagram
    TENANTS ||--o{ EITAA_ACCOUNTS : "دارد"
    EITAA_ACCOUNTS ||--o{ EITAA_GROUPS : "مدیریت"
    EITAA_ACCOUNTS ||--o{ EITAA_CHANNELS : "مدیریت"
    CLASSROOMS ||--o| EITAA_GROUPS : "متصل"
    TENANTS ||--o{ EITAA_MESSAGES : "دارد"
    EITAA_ACCOUNTS ||--o{ EITAA_MESSAGES : "ارسال با"
    EITAA_GROUPS ||--o{ EITAA_MESSAGES : "مقصد"
    EITAA_MESSAGES ||--o{ MESSAGE_DELIVERIES : "تحویل"
    TENANTS ||--o{ SCHEDULED_MESSAGES : "دارد"
    TENANTS ||--o{ AUTOMATION_RULES : "دارد"

    EITAA_ACCOUNTS {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar kind "BOT|APP"
        varchar token_encrypted "AES-256-GCM — هرگز خام"
        varchar token_hint "۴ کاراکتر آخر برای نمایش"
        bigint eitaa_account_id
        varchar username
        integer message_credit "اعتبار باقی‌مانده — فقط APP"
        varchar status "ACTIVE|DISABLED|ERROR"
        timestamptz last_verified_at
    }

    EITAA_GROUPS {
        uuid id PK
        uuid tenant_id FK
        uuid account_id FK
        varchar eitaa_chat_id UK "شناسه گروه در ایتا"
        varchar title
        varchar type "GROUP|CHANNEL"
        integer member_count "nullable — ایتا فهرست اعضا نمی‌دهد"
        uuid classroom_id FK "nullable — اتصال به کلاس"
        varchar status
        boolean can_send_messages
        timestamptz last_synced_at
    }

    EITAA_CHANNELS {
        uuid id PK
        uuid tenant_id FK
        uuid account_id FK
        varchar eitaa_chat_id UK
        varchar title
        varchar username
        integer subscriber_count "nullable"
        varchar status
        boolean can_post
    }

    EITAA_MESSAGES {
        uuid id PK
        uuid tenant_id FK
        uuid account_id FK
        varchar channel "BOT_TO_CHAT|APP_TO_USER"
        varchar target_type "GROUP|CHANNEL|USER"
        uuid target_id "nullable برای USER"
        bigint target_eitaa_id
        uuid sender_user_id FK
        text content_text
        bigint eitaa_message_id "شناسه پیام در ایتا — از پاسخ ok"
        varchar delivery_status "QUEUED|SENT|FAILED"
        varchar error_code
        text error_message
        boolean scheduled_native "با پارامتر date ارسال شد؟"
        timestamptz sent_at
    }

    SCHEDULED_MESSAGES {
        uuid id PK
        uuid tenant_id FK
        uuid message_template_id FK
        varchar target_type
        uuid target_id
        timestamptz scheduled_at
        varchar recurrence "NONE|DAILY|WEEKLY|MONTHLY"
        varchar cron_expression
        varchar status "PENDING|SENT|CANCELLED|FAILED"
        integer attempts
        timestamptz last_attempt_at
    }

    AUTOMATION_RULES {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar trigger_type "ASSIGNMENT_PUBLISHED|SUBMISSION_EVALUATED|EXAM_COMPLETED|SCORE_BELOW|SUBSCRIPTION_EXPIRING|SCHEDULE"
        jsonb trigger_config
        varchar action_type "AI_EVALUATE|NOTIFY_TEACHER|NOTIFY_STUDENT|SEND_MESSAGE|GENERATE_REPORT"
        jsonb action_config
        boolean enabled
        integer execution_count
        timestamptz last_executed_at
    }
```

> ### 🔴 تغییرات نسبت به نسخه ۱.۰
>
> | تغییر | دلیل |
> |---|---|
> | جدول `MESSAGE_INBOUND` **حذف شد** | ایتا مسیر دریافت پیام ندارد |
> | `EITAA_BOTS` → `EITAA_ACCOUNTS` با فیلد `kind` | ایتا **دو نوع حساب** دارد: `BOT` (گروه/کانال) و `APP` (کاربر) |
> | `mode: WEBHOOK\|POLLING` **حذف شد** | هیچ‌کدام وجود ندارند |
> | `webhook_secret_encrypted` **حذف شد** | Webhook وجود ندارد |
> | `direction: INBOUND\|OUTBOUND` **حذف شد** | فقط خروجی وجود دارد |
> | `channel: BOT_TO_CHAT \| APP_TO_USER` **اضافه شد** | دو مسیر ارسال متفاوت با قرارداد متفاوت |
> | تریگر `MESSAGE_RECEIVED` **حذف شد** | ممکن نیست |
> | تریگرهای داخلی **اضافه شدند** | موتور قواعد روی رویدادهای سیستم کار می‌کند |

> `EITAA_GROUPS.eitaa_chat_id` یک **UK سراسری** است نه فقط در سطح مستأجر — چون یک گروه ایتا نمی‌تواند به دو مستأجر تعلق داشته باشد. این یک محدودیت عمدی و یک کنترل ضدسوءاستفاده است.

> ⚠️ **`message_credit` فقط برای `kind=APP` معنا دارد.** ایتا در ساخت هر برنامک **۵۰٬۰۰۰ پیام رایگان** می‌دهد
> و به‌ازای هر پیام به هر کاربر **۱ اعتبار** کسر می‌شود. موجودی از پنل ایتایار **دستی** استعلام و ثبت می‌شود —
> ایتا API استعلام موجودی ندارد. اگر این عدد به آستانه برسد، هشدار به Super Admin می‌رود.

---

## ۸. ERD — AI (Provider / Model / Prompt / Usage)

```mermaid
erDiagram
    AI_PROVIDERS ||--o{ AI_MODELS : "دارد"
    AI_PROVIDERS ||--o{ AI_PROVIDER_ASSIGNMENTS : "تخصیص"
    AI_MODELS ||--o{ AI_PROVIDER_ASSIGNMENTS : "تخصیص"
    PROMPT_TEMPLATES ||--o{ PROMPT_VERSIONS : "نسخه"
    PROMPT_TEMPLATES ||--o{ AI_PROMPT_ASSIGNMENTS : "تخصیص"
    PROMPT_VERSIONS ||--o{ AI_PROMPT_ASSIGNMENTS : "تخصیص"
    PROMPT_VERSIONS ||--o{ AI_USAGE_RECORDS : "استفاده‌شده"
    AI_PROVIDERS ||--o{ AI_USAGE_RECORDS : "ارائه‌دهنده"
    AI_MODELS ||--o{ AI_USAGE_RECORDS : "مدل"
    TENANTS ||--o{ AI_USAGE_RECORDS : "مصرف"

    AI_PROVIDERS {
        uuid id PK
        varchar code UK "ANTHROPIC|XAI|OPENAI|GEMINI|OPENROUTER|OLLAMA|CUSTOM"
        varchar name
        varchar type "ANTHROPIC|OPENAI_COMPATIBLE|GOOGLE|CUSTOM"
        varchar base_url
        text api_key_encrypted "AES-256-GCM — هرگز خام"
        varchar api_key_hint "۴ کاراکتر آخر برای نمایش"
        varchar organization
        boolean enabled
        integer priority "کمتر = اولویت بالاتر"
        integer timeout_seconds
        integer max_retries
        boolean is_fallback
        integer fallback_order
        jsonb configuration "تنظیمات اختصاصی Provider"
        text cost_configuration "JSONB"
        jsonb extra_headers
    }

    AI_MODELS {
        uuid id PK
        uuid provider_id FK
        varchar model_id "شناسه دقیق API — در کد hardcode نمی‌شود"
        varchar display_name
        integer context_window
        integer max_output_tokens
        boolean supports_vision
        boolean supports_structured_output
        boolean supports_function_calling
        boolean supports_reasoning
        boolean supports_streaming
        boolean enabled
        numeric input_cost_per_million
        numeric output_cost_per_million
        numeric cached_input_cost_per_million
        varchar currency "USD"
        jsonb capabilities
        timestamptz last_synced_at "آخرین دریافت از API"
    }

    AI_PROVIDER_ASSIGNMENTS {
        uuid id PK
        varchar scope_type "USER|CLASS|TENANT|PLAN|FEATURE|GLOBAL"
        uuid scope_id "NULL فقط برای GLOBAL"
        uuid provider_id FK
        uuid model_id FK "nullable"
        integer priority "برای زنجیره failover"
        boolean enabled
        uuid tenant_id "NULL = تخصیص پلتفرم‌سطح"
        jsonb overrides "temperature, maxTokens, ..."
    }

    PROMPT_TEMPLATES {
        uuid id PK
        varchar code UK "AI_HOMEWORK_CHECKER"
        varchar name
        varchar feature_code FK
        text description
        boolean is_system "قالب پایه غیرقابل حذف"
    }

    PROMPT_VERSIONS {
        uuid id PK
        uuid template_id FK
        integer version_number
        text system_prompt
        text user_prompt
        jsonb variables "تعریف متغیرها و نوعشان"
        jsonb response_schema "برای Structured Output"
        numeric temperature
        integer max_tokens
        varchar status "DRAFT|PUBLISHED|ARCHIVED"
        uuid published_by FK
        timestamptz published_at
        text change_note
    }

    AI_PROMPT_ASSIGNMENTS {
        uuid id PK
        varchar scope_type "USER|CLASS|TENANT|PLAN|FEATURE|GLOBAL"
        uuid scope_id
        uuid template_id FK
        uuid version_id FK "nullable = آخرین نسخه منتشرشده"
        integer priority
        boolean enabled
        uuid tenant_id
    }

    AI_USAGE_RECORDS {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        varchar feature_code
        uuid provider_id FK
        uuid model_id FK
        uuid prompt_version_id FK
        bigint input_tokens
        bigint output_tokens
        bigint cached_tokens
        numeric estimated_cost
        numeric actual_cost
        integer duration_ms
        varchar status "SUCCESS|FAILED|TIMEOUT|RATE_LIMITED|FALLBACK_USED"
        varchar error_code
        uuid fallback_from_provider_id FK
        varchar trace_id
        timestamptz created_at
    }
```

### نکته امنیتی حیاتی

ستون `api_key_encrypted` با `AttributeConverter` جاوایی رمزنگاری/رمزگشایی می‌شود:

- در Entity به‌صورت `String apiKey` دیده می‌شود، ولی در DB فقط `varbinary` رمزشده ذخیره می‌گردد.
- این فیلد **هرگز** در DTO پاسخ قرار نمی‌گیرد. DTO فقط `apiKeyHint` را برمی‌گرداند.
- `toString()` موجودیت Provider به‌صورت دستی نوشته شده و کلید را حذف می‌کند.
- در `docs/security.md` کامل توضیح داده شده.

---

## ۹. ERD — تجاری (Plan / Subscription / Payment / Wallet)

```mermaid
erDiagram
    PLANS ||--o{ PLAN_FEATURES : "دارد"
    FEATURES ||--o{ PLAN_FEATURES : "در"
    PLANS ||--o{ PLAN_LIMITS : "محدودیت"
    PLANS ||--o{ SUBSCRIPTIONS : "خرید"
    TENANTS ||--o{ SUBSCRIPTIONS : "دارد"
    SUBSCRIPTIONS ||--o{ PAYMENTS : "پرداخت"
    SUBSCRIPTIONS ||--o{ INVOICES : "فاکتور"
    COUPONS ||--o{ COUPON_REDEMPTIONS : "استفاده"
    TENANTS ||--o{ COUPON_REDEMPTIONS : "استفاده‌کننده"
    WALLETS ||--o{ WALLET_TRANSACTIONS : "تراکنش"
    TENANTS ||--o| WALLETS : "دارد"
    TENANTS ||--o{ USAGE_COUNTERS : "مصرف"

    PLANS {
        uuid id PK
        varchar code UK "FREE|BASIC|PRO|SCHOOL|ENTERPRISE"
        varchar name "حرفه‌ای"
        text description
        numeric price
        varchar currency "IRT|USD"
        varchar billing_period "MONTHLY|QUARTERLY|YEARLY|LIFETIME"
        integer trial_days
        boolean is_public "قابل نمایش در اپ"
        boolean is_default "پلن پیش‌فرض ثبت‌نام"
        integer sort_order
        boolean active
    }

    FEATURES {
        uuid id PK
        varchar code UK "AI_CHAT|AI_BOOK_ANALYSIS|..."
        varchar name
        varchar category "AI|EITAA|REPORT|MANAGEMENT"
        text description
        boolean default_enabled
    }

    PLAN_FEATURES {
        uuid id PK
        uuid plan_id FK
        varchar feature_code FK
        boolean enabled
        jsonb config "تنظیمات خاص Feature"
    }

    PLAN_LIMITS {
        uuid id PK
        uuid plan_id FK
        varchar metric "maxStudents|maxClasses|maxAIRequests|..."
        bigint limit_value "-1 = نامحدود"
        varchar period "TOTAL|DAILY|MONTHLY|YEARLY"
    }

    SUBSCRIPTIONS {
        uuid id PK
        uuid tenant_id FK
        uuid plan_id FK
        uuid purchased_by_user_id FK
        varchar status "PENDING|ACTIVE|EXPIRED|CANCELLED|SUSPENDED"
        timestamptz start_date
        timestamptz end_date
        boolean auto_renew
        numeric price_paid
        numeric discount_amount
        uuid coupon_id FK
        uuid previous_subscription_id FK "برای ارتقا/تمدید"
        timestamptz cancelled_at
        text cancellation_reason
    }

    PAYMENTS {
        uuid id PK
        uuid tenant_id FK
        uuid subscription_id FK "nullable"
        uuid invoice_id FK "nullable"
        varchar gateway_code "ZARINPAL|IDPAY|STRIPE|MOCK"
        varchar authority "شناسه درگاه"
        varchar reference_id "شناسه پیگیری"
        numeric amount
        varchar currency
        varchar status "PENDING|SUCCESS|FAILED|REFUNDED|CANCELLED"
        jsonb gateway_response
        timestamptz paid_at
        text failure_reason
    }

    INVOICES {
        uuid id PK
        uuid tenant_id FK
        varchar invoice_number UK "INV-1404-000123"
        uuid subscription_id FK
        numeric subtotal
        numeric discount
        numeric tax
        numeric total
        varchar currency
        varchar status "DRAFT|ISSUED|PAID|VOID|REFUNDED"
        timestamptz issued_at
        timestamptz due_at
        jsonb line_items
    }

    COUPONS {
        uuid id PK
        varchar code UK
        varchar discount_type "PERCENTAGE|FIXED"
        numeric discount_value
        numeric max_discount_amount "سقف برای درصدی"
        numeric min_purchase_amount
        integer usage_limit "کل"
        integer usage_limit_per_tenant
        integer used_count
        timestamptz valid_from
        timestamptz valid_until
        uuid applicable_plan_id FK "nullable = همه پلن‌ها"
        boolean active
    }

    COUPON_REDEMPTIONS {
        uuid id PK
        uuid coupon_id FK
        uuid tenant_id FK
        uuid user_id FK
        uuid subscription_id FK
        numeric discount_applied
        timestamptz redeemed_at
    }

    WALLETS {
        uuid id PK
        uuid tenant_id FK UK
        numeric balance
        varchar currency
        timestamptz updated_at
    }

    WALLET_TRANSACTIONS {
        uuid id PK
        uuid wallet_id FK
        uuid tenant_id FK
        varchar type "CREDIT|DEBIT|REFUND|BONUS|PROMOTIONAL|PURCHASED|ADJUSTMENT"
        numeric amount "مثبت یا منفی"
        numeric balance_after
        varchar reference_type "PAYMENT|SUBSCRIPTION|AI_USAGE|ADMIN_ADJUSTMENT"
        uuid reference_id
        uuid performed_by_user_id FK
        text description
    }

    USAGE_COUNTERS {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK "nullable — مصرف در سطح مستأجر"
        varchar metric "AI_REQUESTS|AI_TOKENS|MESSAGES|STORAGE_BYTES|..."
        bigint used_value
        bigint limit_value "کپی از پلن در شروع دوره — برای تاریخ‌نگاری"
        timestamptz period_start
        timestamptz period_end
        bigint reserved_value "برای رزرو قبل از عملیات طولانی"
    }
```

> **نکته طراحی `USAGE_COUNTERS`:** ستون `limit_value` **کپی** محدودیت پلن در لحظه شروع دوره است. چرا؟ اگر Admin وسط ماه محدودیت پلن را از ۱۰۰ به ۵۰ تغییر دهد، نباید کاربری که ۸۰ مصرف کرده ناگهان بدهکار شود. تغییر در دوره **بعدی** اعمال می‌شود. این یک تصمیم محصولی مهم است.
>
> ستون `reserved_value` برای عملیات طولانی AI است: قبل از فراخوانی Provider، مقدار تخمینی رزرو می‌شود؛ اگر فراخوانی شکست خورد، آزاد می‌شود. این از Race Condition در درخواست‌های همزمان جلوگیری می‌کند.

---

## ۱۰. ERD — زیرساخت (File / Audit / Notification / Report)

```mermaid
erDiagram
    FILES {
        uuid id PK
        uuid tenant_id FK "nullable برای فایل‌های پلتفرم"
        varchar storage_provider "LOCAL|MINIO|S3"
        varchar storage_key "مسیر ذخیره‌سازی"
        varchar original_filename
        varchar content_type
        bigint size_bytes
        varchar checksum_sha256
        varchar entity_type "BOOK_COVER|ASSIGNMENT_ATTACHMENT|..."
        uuid entity_id
        uuid uploaded_by FK
        varchar scan_status "PENDING|CLEAN|INFECTED|FAILED"
        timestamptz deleted_at
    }

    AUDIT_LOGS {
        uuid id PK
        uuid tenant_id FK "nullable برای رویدادهای پلتفرم"
        uuid actor_user_id FK
        varchar actor_type "USER|SYSTEM|API"
        varchar action "PROVIDER_CREATED|PLAN_PRICE_CHANGED|..."
        varchar entity_type
        uuid entity_id
        jsonb before_state
        jsonb after_state
        jsonb metadata
        varchar ip_address
        varchar user_agent
        varchar trace_id
        varchar severity "INFO|WARNING|CRITICAL"
        timestamptz created_at
    }

    NOTIFICATIONS {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        varchar type "SUBSCRIPTION_EXPIRING|AI_LIMIT_REACHED|..."
        varchar title
        text body
        varchar channel "IN_APP|EITAA|EMAIL|SMS"
        varchar status "PENDING|SENT|READ|FAILED"
        jsonb payload
        timestamptz read_at
        timestamptz created_at
    }

    SYSTEM_SETTINGS {
        uuid id PK
        varchar key UK
        text value
        varchar value_type "STRING|INT|BOOLEAN|JSON"
        varchar category
        boolean encrypted "اگر true، مقدار رمزنگاری‌شده"
        text description
        uuid updated_by FK
        timestamptz updated_at
    }

    SCHEDULER_JOBS {
        uuid id PK
        varchar job_name UK
        varchar job_group
        varchar cron_expression
        varchar status "RUNNING|PAUSED"
        timestamptz last_run_at
        timestamptz next_run_at
        integer last_duration_ms
        varchar last_status "SUCCESS|FAILED"
        text last_error
        integer consecutive_failures
    }

    SUPPORT_TICKETS {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        varchar subject
        text description
        varchar priority "LOW|NORMAL|HIGH|URGENT"
        varchar status "OPEN|IN_PROGRESS|ANSWERED|CLOSED"
        uuid assigned_to FK
        timestamptz closed_at
    }
```

---

## ۱۱. جداول اتصالی (Junction) — فهرست کامل

| جدول | طرفین | توضیح |
|---|---|---|
| `tenant_members` | users ↔ tenants | با `role` |
| `user_roles` | users ↔ roles | نقش پلتفرم‌سطح |
| `role_permissions` | roles ↔ permissions | |
| `class_students` | classrooms ↔ students | با `roll_number` |
| `class_teachers` | classrooms ↔ users | با `role` |
| `book_chapters` ↔ `book_pages` | از طریق `book_id` + بازه صفحه | |
| `assignment_attachments` | assignments ↔ files | |
| `assignment_submission_files` | submissions ↔ files | |
| `plan_features` | plans ↔ features | |
| `coupon_redemptions` | coupons ↔ tenants | |

---

## ۱۲. استراتژی Migration (Flyway)

```text
backend/src/main/resources/db/migration/
├── V1__baseline_extensions_and_helpers.sql
├── V2__identity_and_tenancy.sql
├── V3__roles_and_permissions.sql
├── V4__features_and_plans.sql
├── V5__subscriptions_payments_wallet.sql
├── V6__ai_providers_models.sql
├── V7__prompt_templates_versions.sql
├── V8__ai_usage_and_assignments.sql
├── V9__classrooms_students.sql
├── V10__books_and_pages.sql
├── V11__assignments_and_submissions.sql
├── V12__exams.sql
├── V13__eitaa_bots_groups_channels.sql
├── V14__messages_and_automation.sql
├── V15__files_audit_notifications.sql
├── V16__usage_counters.sql
├── V17__seed_roles_permissions_features.sql
├── V18__seed_default_plans.sql
└── V19__seed_default_prompts.sql
```

### وضعیت واقعی پیاده‌سازی (پایان گام ۱ فاز ۳)

⚠️ **فهرست بالا نقشه راه است، نه وضعیت فعلی.** فاز ۱ پنج Migration نخست را
ساخت و **شماره‌گذاری دو مورد جابه‌جا شد**؛ فازهای ۲ و ۳ هم Migrationهای
خودشان را با همان قاعده «بزرگ‌ترین شماره موجود + ۱» ساختند:

| فایل واقعی | وضعیت | جدول‌ها و اشیای ساخته‌شده |
|---|---|---|
| `V1__baseline_extensions_and_helpers.sql` | ✅ ساخته شد | افزونه‌های `pgcrypto` و `pg_trgm` · تابع `set_updated_at()` |
| `V2__identity_and_tenancy.sql` | ✅ ساخته شد | `users`, `tenants`, `tenant_members`, `eitaa_identities` + تریگرهای `updated_at` |
| `V3__roles_and_permissions.sql` | ✅ ساخته شد | `roles`, `permissions`, `role_permissions`, `user_roles` |
| `V4__refresh_tokens.sql` | ✅ ساخته شد | `refresh_tokens` — **در نقشه راه نبود** |
| `V5__audit_and_settings.sql` | ✅ ساخته شد | `audit_logs`, `system_settings`, `scheduler_jobs` + تابع `prevent_audit_mutation()` — **در نقشه راه نبود** |
| `V6__event_publication_registry.sql` | ✅ ساخته شد | جدول رجیستری Spring Modulith (`event_publication`) — **در نقشه راه نبود** |
| `V7__eitaa_write_access.sql` | ✅ ساخته شد | `eitaa_write_access` — نتیجه تأیید دسترسی نوشتن ربات (گام ۱ فاز ۲) |
| `V8__subjects_and_classrooms.sql` | ✅ ساخته شد | `subjects`, `classrooms` + کلید خارجی ترکیبی و ایندکس‌های جزئی (گام ۱ فاز ۳) — **در نقشه راه نبود** |
| `V9__fix_classroom_search_index.sql` | ✅ ساخته شد | اصلاح ایندکس تری‌گرام: `gin (name)` ← `gin (lower(name))` — **V8 را ویرایش نکردیم، به حکم قاعده ۱ پایین** |
| `V10__students_and_enrollment.sql` | ✅ ساخته شد | `students`, `class_students` + `uk_classrooms_tenant_id` (گام ۲ فاز ۳) |
| `V11__admin_password_hash_capacity.sql` | ✅ ساخته شد | ظرفیت هش رمز ادمین |
| `V12__files_and_books.sql` | ✅ ساخته شد | `files`, `books`, `book_chapters`, `book_pages` (گام ۱ فاز ۶) — **شماره نقشه راه V10 بود** |
| `V13__ai_providers_models_usage.sql` | ✅ ساخته شد | `ai_providers`, `ai_models`, `ai_usage_records` (گام ۱ فاز ۵) |
| `V14__features_and_overrides.sql` | ✅ ساخته شد | `features`, `feature_overrides` (فاز ۴) |
| `V15__assignments_and_submissions.sql` | ✅ ساخته شد | `assignments`, `assignment_submissions`, stub `ai_evaluations` (گام ۱ فاز ۷) — **شماره نقشه راه V11 بود** |
| `V16__exams.sql` | ✅ ساخته شد | آزمون‌ها (فاز ۸ گام ۱) |
| `V17__eitaa_groups_and_scheduled_messages.sql` | ✅ ساخته شد | گروه/پیام زمان‌بندی ایتا (فاز ۹) |
| `V18__plans_subscriptions_invoices.sql` | ✅ ساخته شد | پلن، اشتراک، فاکتور + seed (فاز ۱۰ گام ۱) |
| `V19__report_aggregation_indexes.sql` | ✅ ساخته شد | ایندکس تجمیع گزارش/نمودار (فاز ۱۱ گام ۱) |
| `V20__ai_provider_assignments.sql` | ✅ ساخته شد | `ai_provider_assignments` (فاز ۵ — ProviderResolver) |
| `V21__prompts.sql` | ✅ ساخته شد | `prompts` / `prompt_versions` / `prompt_assignments` + seed `AI_CHAT_SYSTEM` |
| `V22__content_analyses_and_storage.sql` | ✅ ساخته شد | `content_analyses` + سقف اختیاری `storageBytes` (فاز ۶ گام ۲) |
| `V23__ai_evaluations_and_teacher_reviews.sql` | ✅ ساخته شد | گسترش `ai_evaluations` + `teacher_reviews` Append-Only (فاز ۷ گام ۲) |
| `V24__exam_blueprints.sql` | ✅ ساخته شد | طرح تولید آزمون (فاز ۸ گام ۲) |
| `V25__eitaa_channels_automation.sql` | ✅ ساخته شد | کانال · automation_rules · message_credits (فاز ۹ گام ۲) |
| `V26__billing_coupons_wallets_plan_features.sql` | ✅ ساخته شد | کوپن/کیف‌پول/ویژگی پلن (فاز ۱۰) |

> **⚠️ چرا V9 به‌جای ویرایش V8؟** ایندکس تری‌گرامی که V8 ساخته بود روی
> `name` خام بود، ولی کوئری `lower(name) like :pattern` است و پستگرس آن
> دو را به هم نگاشت نمی‌کند. ایندکس هرگز انتخاب نمی‌شد — با `EXPLAIN` و
> `enable_seqscan = off` هم پلن `Seq Scan` ماند. V8 در دیتابیس توسعه
> اعمال شده بود، پس طبق قاعده ۱ ویرایشش checksum را می‌شکست؛ V9 همان
> ایندکس را `DROP` و درستش را می‌سازد. تست
> `searchIndexMatchesTheQueryExpression` این را قفل می‌کند.

**چرا فاز ۱ جابه‌جا شد؟** فاز ۱ به این دو جدول نیاز واقعی داشت: بدون `refresh_tokens`
چرخه احراز هویت کامل نمی‌شود، و بدون `audit_logs` رویدادهای امنیتی
(`SecurityAuditLog`) جایی برای نوشتن ندارند. جدول‌های Feature/Plan/Subscription
به فازهای ۴ و ۱۰ موکول شدند.

```text
⇒ شماره‌گذاری واقعی با نقشه راه بالا هم‌راستا نشد و مهم هم نیست:
   Flyway تنها ترتیب عددی را می‌خواند، نه نام را.

⇒ ردیف‌های V4 به بعد در نقشه راه بالا دیگر معتبر نیستند.
   عدد درست همیشه «بزرگ‌ترین عدد موجود + ۱» است.
```

> **⚠️ این جابه‌جایی عمداً اینجا ثبت شد.** قاعده ۱ پایین می‌گوید Migration
> اجراشده ویرایش نمی‌شود؛ اگر این اختلاف ثبت نمی‌شد، نفر بعدی فهرست نقشه راه
> را می‌خواند و `V6__ai_providers_models.sql` را می‌ساخت — در حالی که
> `V6` باید جدول‌های Feature/Plan را بسازد. یک Migration با شماره تکراری
> یا نام بی‌ربط، مهاجرت production را می‌شکند.

### قواعد Migration

1. **هرگز Migration اجراشده را ویرایش نکن.** برای تغییر، Migration جدید بساز.
2. **Migrationها در `prod` باید Backward-Compatible باشند** (چند ثانیه Downtime قابل قبول است، ولی داده نباید از دست برود).
3. **Seed داده‌های سیستمی** (`roles`, `permissions`, `features`, پلن FREE، پرامپت‌های پایه) در Migration انجام می‌شود — نه در کد — چون بخشی از Schema منطقی است.
4. **داده‌های نمایشی (Demo)** هرگز در Migration نیستند؛ در `scripts/demo-data.sql` جداگانه‌اند.
5. برای تغییرات مخرب (حذف ستون)، الگوی **Expand-Contract** در دو Release:
   - Release N: ستون جدید اضافه، هر دو نوشته شوند
   - Release N+1: کد از ستون قدیم استفاده نکند
   - Release N+2: ستون قدیم حذف شود

### Migration پایه — افزونه‌ها و توابع کمکی

```sql
-- V1__baseline_extensions_and_helpers.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";   -- جست‌وجوی متنی فارسی

-- تریگر به‌روزرسانی updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

> **نکته:** از `uuid-ossp` استفاده نمی‌کنیم چون UUID v7 در آن نیست و ما UUID را در Java تولید می‌کنیم. `pgcrypto` فقط برای توابع کمکی و شاید `gen_random_bytes` لازم است.

---

## ۱۳. اندازه‌گیری و نگهداشت

| جدول | نرخ رشد تخمینی | استراتژی |
|---|---|---|
| `ai_usage_records` | بسیار بالا (هر درخواست AI) | Partition ماهانه در فاز ۱۲ + آرشیو پس از ۱۲ ماه |
| `messages` | بالا | آرشیو پس از ۶ ماه |
| `audit_logs` | متوسط | نگهداری ۲۴ ماه، سپس آرشیو سرد |
| `notifications` | بالا | حذف خوانده‌شده‌های قدیمی‌تر از ۹۰ روز |
| `usage_counters` | پایین | نگهداری دائمی (سابقه مصرف) |
| `exam_answers` | متوسط | همراه آزمون |

این‌ها در فاز اول فقط **مستند** می‌شوند و در فاز ۱۲ پیاده‌سازی می‌گردند. اما ایندکس‌های لازم از ابتدا ساخته می‌شوند تا مهاجرت دردناک نباشد.
