# ثبت راستی‌آزمایی واقعیت‌های بیرونی

> بخش ۷۳ خواسته‌ها («هیچ API یا قابلیتی را حدس نزن») · نسخه 1.0 · 2026-09-28

---

## چرا این سند وجود دارد

خواسته‌ها صریح می‌گوید: **هیچ API یا قابلیتی حدس زده نشود.**

اما یک سند معماری، ناگزیر ادعاهایی درباره دنیای بیرون دارد: «فلان API این متد را دارد»،
«فلان سرویس این‌طور کار می‌کند». اگر این ادعاها با واقعیت نخوانند، **کل طراحی باطل می‌شود** —
و بدترین حالت این است که کسی نفهمد کدام ادعا تأییدشده و کدام حدس بوده.

این سند آن ابهام را رفع می‌کند. **هر واقعیت بیرونی که در طراحی به آن تکیه کرده‌ایم، اینجا ثبت شده است:**

| ستون | معنا |
|---|---|
| **ادعا** | واقعیتی که طراحی به آن وابسته است |
| **وضعیت** | ✅ تأییدشده · ⚠️ نیازمند آزمون · ❌ رد‌شده |
| **منبع** | دقیقاً از کجا آمده |
| **اثر** | کدام سند/فاز به آن وابسته است |

---

## ۱. ✅ واقعیت‌های تأییدشده — ایتا

### ۱.۱ Mini App SDK

| ادعا | وضعیت | منبع | اثر |
|---|---|---|---|
| اسکریپت SDK: `<script src="https://developer.eitaa.com/eitaa-web-app.js">` | ✅ | `docs/Develop/JsSDK/` | `mini-app.md`, `eitaa.md` |
| این فایل باید **مستقیماً از آدرس رسمی** بارگذاری شود — Self-host ممنوع | ✅ | `docs/Develop/JsSDK/` (هشدار صریح) | `mini-app.md` §۴ |
| شیء Global: **`window.Eitaa.WebApp`** | ✅ | `docs/Develop/JsSDK/` + `docs/FAQ/` | `mini-app.md`, `eitaa.md` |
| `initData` رشته خام است و اعتبارسنجی **باید در سرور** انجام شود | ✅ | `docs/Develop/JsSDK/` | `eitaa.md` §۷ |
| `initDataUnsafe` قابل اعتماد **نیست** | ✅ | `docs/Develop/JsSDK/` | `mini-app.md` |
| فیلدهای `initData`: `query_id`, `user`, `chat_type`, `chat_instance`, `start_param`, `auth_date`, `device_id`, `hash` | ✅ | `docs/Develop/JsSDK/` | `eitaa.md` §۲.۵ |
| `user.id` تا **۵۲ بیت** اهمیت دارد | ✅ | `docs/Develop/JsSDK/` | `eitaa.md` §۸ — از `Long` استفاده می‌کنیم |
| `HapticFeedback` **وجود دارد** | ✅ | `docs/Develop/JsSDK/` | `mini-app.md` §۴ |
| `requestWriteAccess([cb])` **وجود دارد** — اجازه ارسال پیام | ✅ | `docs/Develop/JsSDK/` | `eitaa.md` §۵.۲ |
| `requestContact([cb])` — شماره با hash قابل اعتبارسنجی | ✅ | `docs/Develop/JsSDK/` | `eitaa.md` §۵.۳ |
| `start_param` = پارامتر Deep Link | ✅ | `docs/Develop/JsSDK/` | `eitaa.md` §۲.۵, فاز ۹ |
| `isVersionAtLeast(v)` برای Feature Detection | ✅ | `docs/Develop/JsSDK/` | `mini-app.md` §۴ |
| متغیرهای CSS با پیشوند `--tg-` | ✅ | `docs/Develop/JsSDK/` | `mini-app.md` §۹ |
| **پلتفرم: فقط اندروید + دسکتاپ ویندوز** | ✅ | `docs/QuickStart/` | `mini-app.md` §۴.۵ — **محدودیت محصولی** |
| iOS و مرورگر پشتیبانی **نمی‌شوند** | ✅ | `docs/QuickStart/` | گارد پلتفرم |
| HTTPS **اجباری**، Redirect **ممنوع** | ✅ | `docs/QuickStart/` + `ArchitectureOverview` | `mini-app.md` §۴ |
| **Push Notification ممکن نیست** | ✅ | `docs/FAQ/` (پاسخ صریح «خیر») | `eitaa.md` §۱۱.۱ |
| **قالب الگوریتم HMAC:** `secret = HMAC(key="WebAppData", msg=token)` | ✅ | `docs/Develop/AuthorizationViaHash/` | `eitaa.md` §۷.۲ |
| بردار تست: توکن `5768337691:AAG...` → `aa492a44...a449` | ✅ | `docs/Develop/AuthorizationViaHash/` | تست `EitaaSecretKeyDerivationTest` |

> ✅ **این آزمون اجرا شد و PASS شد** (پیش‌تر ⚠️ بود: «در این محیط بازتولید نشد»).
> خروجی واقعی:
>
> ```text
> $ printf '%s' '5768337691:AAGDAe6rjxu1cUgxK4BizYi--Utc3J9v5AU' \
>     | openssl dgst -sha256 -mac HMAC -macopt key:WebAppData
> SHA2-256(stdin)= aa492a44bdf019c759defb1698c1d77690189973945491a756051cdc1207a449
> ```
>
> `aa492a44…a449` دقیقاً همان بردار مستندات است. یعنی جهت کلید
> (`key = "WebAppData"`, `msg = token`) **قطعی است** و دیگر فرض نیست.
> این مستقل از پیاده‌سازی خودمان تأیید شد — با ابزار بیرونی `openssl`، نه با
> کدی که خودمان نوشته‌ایم. (اگر فقط تست خودمان را معیار می‌گرفتیم، یک اشتباه
> مشترک در کد و تست می‌توانست از چشم بیفتد.)
>
> ⚠️ **اما هنوز هیچ کد ایتایی در مخزن نیست** — نه `EitaaSecretKeyDerivationTest`
> و نه هیچ کلاس دیگری (جست‌وجو برای `Eitaa*` و بردار HMAC در `src/` خالی برمی‌گردد).
> این درست است، چون یکپارچه‌سازی ایتا کار **فاز ۲** است و فاز ۱ نباید آن را شروع کند.
> پس این آزمون در فاز ۲ به‌صورت تست خودکار نوشته می‌شود.

### ۱.۲ Bot API ایتایار

| ادعا | وضعیت | منبع | اثر |
|---|---|---|---|
| قالب آدرس: `https://eitaayar.ir/api/{TOKEN}/{METHOD_NAME}` | ✅ | PDF رسمی «راهنمای API ایتایار» | `eitaa.md` §۴.۱ |
| نام متد **حساس به بزرگی/کوچکی نیست** | ✅ | همان PDF | `EitaaHttpClient` |
| هر دو متد `GET` و `POST` پشتیبانی می‌شوند | ✅ | همان PDF | `EitaaHttpClient` |
| چهار قالب پارامتر: query · JSON · form-urlencoded · multipart | ✅ | همان PDF | `EitaaHttpClient` |
| **اولین پارامتر هر پاسخ `ok` است** — همیشه چک شود | ✅ | همان PDF (تأکید صریح) | `EitaaHttpClient` — چک اجباری |
| **فقط ۳ متد وجود دارد:** `getMe`, `sendMessage`, `sendFile` | ✅ | همان PDF | **بازطراحی کامل** |
| `sendMessage` پارامترها: `chat_id`, `text`, `title`, `disable_notification`, `reply_to_message_id`, `date`, `pin`, `viewCountForDelete` | ✅ | همان PDF | `EitaaSendOptions` |
| `date` = Unix Timestamp → **زمان‌بندی بومی** | ✅ | همان PDF | فاز ۹ |
| `pin` = سنجاق پس از ارسال | ✅ | همان PDF | فاز ۹ |
| `viewCountForDelete` = حذف خودکار پس از N بازدید | ✅ | همان PDF | فاز ۹ |
| `sendFile` پارامترها: `file`, `caption` | ✅ | همان PDF | `EitaaAdapter` |
| گیف: پسوند → `.gif` · استیکر: پسوند → `.webp` | ✅ | همان PDF | `EitaaAdapter` |
| پاسخ موفق شامل `message_id` است | ✅ | همان PDF | `eitaa_message_id` در DB |
| آزمون سریع: `https://eitaayar.ir/testApi` | ✅ | همان PDF | فاز ۲ |

### ۱.۳ API برنامک (ارسال به کاربر)

| ادعا | وضعیت | منبع | اثر |
|---|---|---|---|
| `POST https://eitaayar.ir/api/app/sendMessage` با `{token, chat_id, text}` | ✅ | `docs/Develop/SendMassage/` | مسیر ب |
| پاسخ موفق: `{"ok": true, "result": "success"}` — **هر چیز دیگری = شکست** | ✅ | `docs/Develop/SendMassage/` | `EitaaAppMessageAdapter` |
| `chat_id` اینجا **آیدی کاربر** است، نه گروه | ✅ | `docs/Develop/SendMassage/` | تفکیک دو مسیر |
| نیازمند **دسترسی ارسال** از کاربر (Start یا لینک مستقیم) | ✅ | `docs/Develop/SendMassage/` | `requestWriteAccess` |
| **۵۰٬۰۰۰ پیام رایگان** در ساخت برنامک | ✅ | `docs/Develop/SendMassage/` | `message_credit` |
| **۱ اعتبار** به‌ازای هر پیام به هر کاربر | ✅ | `docs/Develop/SendMassage/` | مدل هزینه |

### ۱.۴ سرویس اعتبارسنجی از راه دور (استفاده نمی‌کنیم)

| ادعا | وضعیت | منبع | اثر |
|---|---|---|---|
| `POST https://eitaayar.ir/api/app/verify` با `{token, hash, ip}` | ✅ | `docs/Develop/ValidateHash/` | `EitaaHashVerifier` |
| اعتبارسنجی **یک‌بارمصرف** است | ✅ | همان | دلیل عدم استفاده |
| فقط برای درخواست **کمتر از ۱ دقیقه** | ✅ | همان | دلیل عدم استفاده |
| **حداکثر ۲۰ درخواست در ثانیه** و سپس مسدود شدن IP | ✅ | همان (هشدار صریح) | 🔴 دلیل اصلی عدم استفاده |
| استفاده از آن **اجباری نیست** | ✅ | همان | تصمیم نهایی |

### ۱.۵ ❌ قابلیت‌هایی که رد شدند

| ادعای نسخه ۱.۰ | وضعیت | منبع رد | جایگزین |
|---|---|---|---|
| Bot API متد `getUpdates` دارد | ❌ **رد شد** | PDF رسمی — فهرست کامل متدها | — (ممکن نیست) |
| Bot API متد `setWebhook` دارد | ❌ **رد شد** | همان | — (ممکن نیست) |
| Long-Polling برای دریافت پیام | ❌ **رد شد** | همان — هیچ متدی برای آن نیست | — (ممکن نیست) |
| دریافت پیام از گروه | ❌ **رد شد** | همان | فرم Mini App |
| `editMessageText` / `deleteMessage` | ❌ **رد شد** | همان | پیام اصلاحی + `viewCountForDelete` |
| `pinChatMessage` برای پیام دلخواه | ❌ **رد شد** | همان | فقط `pin` روی ارسال همان لحظه |
| `getChatMembersCount` / `getChatAdministrators` | ❌ **رد شد** | همان | ورود دستی معلم |
| `sendPoll` | ❌ **رد شد** | همان | آزمون در Mini App |
| دکمه شیشه‌ای (Inline Keyboard) | ❌ **رد شد** | همان | لینک `start_param` |
| Callback Query | ❌ **رد شد** | همان | — |
| نام Global: `window.EitaaWebApp` | ❌ **رد شد** | `docs/Develop/JsSDK/` | `window.Eitaa.WebApp` |

---

## ۲. ⚠️ نیازمند آزمون واقعی — فاز ۲

این موارد در مستندات **پیدا نشدند**. صریحاً علامت‌گذاری می‌شوند تا کسی اشتباهاً آن‌ها را تأییدشده نپندارد.

```text
[ ] نام صحیح متد سوم: sendFile یا sendDocument؟
      ↑ مستندات متناقض است: فهرست ابتدایی «sendDocument» می‌گوید،
        شرح کامل «sendFile». هر دو آزموده و در EitaaProperties ثابت می‌شود.
[ ] محدودیت نرخ ارسال (rate limit) واقعی چقدر است؟
[ ] حداکثر طول متن پیام چند کاراکتر است؟
[ ] حداکثر حجم فایل چقدر است؟
[ ] آیا ربات می‌تواند به کانال خصوصی ارسال کند؟
[ ] پارامتر date در کدام منطقه زمانی تفسیر می‌شود — UTC یا محلی؟
[ ] آیا viewCountForDelete روی پیام گروه کار می‌کند یا فقط کانال؟
[ ] رفتار requestWriteAccess در نسخه دسکتاپ ویندوز
[ ] آیا start_param در همه انواع لینک پر می‌شود؟
[ ] رفتار دقیق BottomButton — متدهای کامل (setText, enable, disable…)
[ ] رفتار ربات وقتی از گروه حذف می‌شود — کد خطای دقیق
```

> **سیاست:** تا آزمون واقعی، مقدار **محافظه‌کارانه** فرض می‌شود و در `EitaaProperties`
> **قابل تنظیم** است. هیچ‌کدام در کد به‌عنوان ثابت نوشته نمی‌شود.

> **وضعیت پس از فاز ۲ (بخش ۱۱):** این فهرست **همچنان باز است** — هیچ‌کدام از این
> موارد با دسترسی به سرویس واقعی ایتا تأیید نشده. دو مورد از آن‌ها اما از «نامعلوم»
> به «تعریف‌شدهٔ سمت ما» تغییر وضعیت دادند: رفتار `start_param` (بخش ۱۱.۲) و
> منطقه زمانی `date` (که آزمون `ScheduledSendUsesDateParamTest` **فرض ما** را
> قفل می‌کند، نه رفتار ایتا را). تفاوت این دو را نباید از دست داد: قفل‌کردن یک فرض
> یعنی «اگر ایتا جور دیگری رفتار کند، یک جا و به‌طور قابل‌تغییر عوض می‌شود» — نه
> «تأیید شد».

---

## ۳. ✅ واقعیت‌های تأییدشده — محیط توسعه

| ادعا | وضعیت | نحوه تأیید | اثر |
|---|---|---|---|
| `JAVA_HOME` سیستم روی **JDK 21** است | ✅ | `echo $JAVA_HOME` | `development.md` §۲ |
| JDK 25 در `C:\Program Files\Java\jdk-25.0.2` نصب است | ✅ | بررسی فایل‌سیستم | Toolchain |
| **`StructuredTaskScope` در JDK 25 هنوز Preview است** | ✅ | **کامپایل واقعی — خطای بیلد** | ❌ استفاده نمی‌شود |
| `ScopedValue` در JDK 25 **نهایی** است | ✅ | کامپایل موفق | `TenantContext` |
| Virtual Threads نهایی است | ✅ | کامپایل موفق | `Executors.newVirtualThreadPerTaskExecutor()` |
| Record Patterns نهایی است | ✅ | کامپایل موفق | نگاشت DTO |

> **روش تأیید:** این‌ها **حدس از دانش عمومی نبودند** — با کامپایل واقعی در همین محیط آزموده شدند.
> خطای `StructuredTaskScope is a preview API and is disabled by default` مستقیماً دیده شد
> و طراحی بر همان اساس اصلاح شد.

### فرض‌های تأییدنشده محیط (⚠️)

| ادعا | وضعیت | اثر |
|---|---|---|
| نسخه دقیق Spring Boot 4.1.1 روی Maven Central موجود است | ⚠️ | فاز ۱ — با `mvn dependency:resolve` تأیید می‌شود |
| سازگاری Spring Modulith 2.1.1 با Boot 4.1.1 | ⚠️ | فاز ۱ |
| springdoc-openapi 3.1.1 با Boot 4.1.1 سازگار است | ⚠️ | فاز ۱ |

> **اگر هر یک ناسازگار بود، در فاز ۱ نسخه اصلاح می‌شود — نه اینکه طراحی بشکند.**

---

## ۴. ⚠️ واقعیت‌های تأییدنشده — Providerهای AI

> **این‌ها در فاز ۵ پیش از هر پیاده‌سازی، مستقیماً از مستندات رسمی هر Provider تأیید می‌شوند.**

| Provider | چه چیزی باید تأیید شود |
|---|---|
| Anthropic | نام دقیق Header احراز هویت · ساختار درخواست/پاسخ · نام فیلدهای Structured Output |
| xAI (Grok) | سازگاری کامل با قالب OpenAI · نام مدل‌ها |
| OpenAI | قالب `response_format` · مدل‌های موجود |
| Google Gemini | ساختار متفاوت درخواست (نه OpenAI-compatible) · نحوه احراز هویت |
| OpenRouter | نام Headerهای اضافی (`HTTP-Referer`, `X-Title`) |
| Ollama | قالب API محلی · مدیریت مدل‌ها |

**رویکرد ما:**
```text
۱. هیچ Endpoint یا نام فیلدی در کد ثابت (hardcode) نمی‌شود
۲. هر Provider یک Adapter مستقل دارد
۳. تا تأیید، MockAiProvider همه تست‌ها را پوشش می‌دهد
۴. اگر Providerی تأیید نشد، در Model Catalog غیرفعال می‌ماند — نه Fake
```

---

## ۵. ⚠️ واقعیت‌های تأییدنشده — درگاه پرداخت

| درگاه | چه چیزی باید تأیید شود |
|---|---|
| زرین‌پال | Endpoint دقیق · ساختار `authority` · نحوه Verify · کدهای خطا |
| IDPay | همان |
| Stripe | همان (برای فروش بین‌المللی آینده) |

> **رویکرد:** `PaymentGateway` interface + `MockPaymentGateway` کامل.
> Adapter واقعی فقط پس از تأیید مستندات رسمی نوشته می‌شود.
> **هیچ‌وقت با فرض «مثل فلان درگاه است» پیاده نمی‌شود.**

---

## ۶. خلاصه اثرها

| سند | اثر راستی‌آزمایی |
|---|---|
| `docs/eitaa.md` | **بازنویسی کامل** — نسخه ۱.۰ → ۲.۰ |
| `docs/mini-app.md` | SDK واقعی + فهرست کامل متدها + محدودیت پلتفرم + write access + start_param |
| `docs/architecture.md` | حذف Webhook از نمودار · افزودن نکته «فقط خروجی» |
| `docs/database.md` | حذف `MESSAGE_INBOUND` · `EITAA_BOTS` → `EITAA_ACCOUNTS` · تریگرهای جدید |
| `docs/api.md` | حذف `POST /eitaa/webhook/{botId}` |
| `docs/roadmap.md` | فاز ۲ (حذف دریافت Update) · فاز ۷ (پاسخ در Mini App) · فاز ۹ (تریگرهای داخلی) |
| `docs/development.md` | `EITAA_APP_TOKEN` · حذف `EITAA_WEBHOOK_SECRET` |

---

## ۷. فرایند به‌روزرسانی این سند

```text
۱. هر واقعیت بیرونی جدید → یک ردیف در جدول مربوطه
۲. هرگز یک ردیف ✅ را بدون منبع جدید به ⚠️ تغییر نده
۳. هرگز یک ردیف ⚠️ را بدون آزمون واقعی به ✅ ارتقا نده
۴. اسکریپت scripts/check-eitaa-docs.sh ماهانه صفحات رسمی را بررسی می‌کند
۵. اگر مستندات ایتا تغییر کند، این سند **اول** به‌روز می‌شود، بعد کد
```

> **قاعده طلایی:** اگر در این سند ردیفی وجود ندارد، آن واقعیت **تأیید نشده** است —
> حتی اگر بدیهی به نظر برسد.

---

## ۸. ثبت فاز ۱ — پشته فنی

> این بخش در پایان فاز ۱ (زیرساخت) افزوده شد. دو دسته چیز اینجاست:
> آنچه **با کامپایل واقعی تأیید شد**، و آنچه **هنوز تأیید نشده**.

### ۸.۰ ⚠️ هشدار مهم درباره وضعیت تأیید

```text
در زمان نوشتن این بخش، هیچ اجرای موفقی از «mvn clean verify» انجام نشده است.

دلیل: ابزار اجرای دستور (Shell) در این محیط در دسترس نبود.

پس هر ردیف ✅ در بخش ۸.۱ صرفاً یعنی «کامپایلر/IDE این کد را پذیرفت»،
نه «بیلد سبز است». تفاوت این دو مهم است و نباید مبهم بماند.
تأیید نهایی فاز ۱ = یک اجرای سبز mvn clean verify روی ماشین توسعه‌دهنده.
```

### ۸.۱ ✅ تأییدشده با کامپایلر/IDE

| ادعا | وضعیت | نحوه تأیید | اثر |
|---|---|---|---|
| `org.springframework.lang.NonNull` در Spring Framework 7 **Deprecated** است | ✅ | هشدار واقعی IDE روی `JwtAuthenticationFilter` | آن annotation حذف شد |
| ایمپورت‌های Spring Security 7 در `SecurityConfig` معتبرند | ✅ | بدون خطای IDE | `SecurityConfig` |
| `CookieCsrfTokenRepository`, `@EnableMethodSecurity`, `SessionCreationPolicy` معتبرند | ✅ | بدون خطای IDE | زنجیره ادمین |
| `ApplicationModule` در Modulith 2.1.1 متد `getName()` **ندارد** | ✅ | **خطای کامپایل واقعی** | `ApplicationModulesTest` بازنویسی شد |
| APIهای ArchUnit 1.5.1 (`noFields`, `beRecords`, `DO_NOT_INCLUDE_TESTS`) معتبرند | ✅ | بدون خطای IDE | `ArchitectureTest` |
| `AvailableSettings.MULTI_TENANT` و `MULTI_TENANT_IDENTIFIER_RESOLVER` در Hibernate 7 هستند | ✅ | بدون خطای IDE | `HibernateTenantConfig` |
| `@JdbcTypeCode(SqlTypes.JSON)` روی فیلد `String` کامپایل می‌شود | ✅ | بدون خطای IDE | `Tenant.settings`, `AuditLog` |

> **نکته روش‌شناسی:** ردیف `ApplicationModule.getName()` نمونه‌ای از ارزش این سند است:
> یک فرض معقول بود («هر ماژول نام دارد پس getName دارد») که **کامپایلر رد کرد**.
> اگر آن فرض بی‌صدا در کد می‌ماند، بیلد اول شکست می‌خورد و علتش هم مبهم بود.

### ۸.۲ ⚠️ نیازمند تأیید در نخستین بیلد

| ادعا | وضعیت | چه چیزی تأییدش می‌کند |
|---|---|---|
| نام استارتر وب در Boot 4: `spring-boot-starter-webmvc` | ⚠️ | `mvn dependency:resolve` — اگر نبود، به `spring-boot-starter-web` تغییر کند |
| نسخه Spring Boot 4.1.1 روی Maven Central موجود است | ⚠️ | همان |
| سازگاری Spring Modulith 2.1.1 با Boot 4.1.1 | ⚠️ | `mvn clean verify` |
| سازگاری springdoc-openapi 3.1.1 با Boot 4.1.1 | ⚠️ | همان |
| `ScopedValue.isBound()` و `ScopedValue.get()` در JDK 25 نهایی‌اند | ⚠️ | کامپایل `TenantContext.peek()` |
| `maven-toolchains-plugin` بدون `~/.m2/toolchains.xml` بیلد را **متوقف** می‌کند | ⚠️ | نخستین `mvn` — راهنما در `development.md` §۲ |
| تگ‌های Docker: `maven:3.9-eclipse-temurin-25` و `eclipse-temurin:25-jre-alpine` موجودند | ⚠️ | `docker build` |
| `@Container @ServiceConnection` روی فیلد static پیش از ساخت کانتکست، کانتینر را بالا می‌آورد | ⚠️ | `TenantIsolationTest` |
| `hibernate.ddl-auto: validate` با ستون‌های `JSONB` نگاشت‌شده به `String` سازگار است | ⚠️ | همان — **پرجمعیت‌ترین ریسک این فاز** |

> **درباره ردیف آخر:** نگاشت `JSONB` به `String` با `@JdbcTypeCode(SqlTypes.JSON)` روی
> PostgreSQL 17 و Hibernate 7 باید کار کند، ولی اعتبارسنجی Schema حساس است.
> اگر شکست خورد، راه‌حل شناخته‌شده `columnDefinition = "jsonb"` صریح روی ستون است.

### ۸.۳ ⚠️ رفتاری که کد به آن تکیه می‌کند و باید آزموده شود

| ادعا | وضعیت | اثر |
|---|---|---|
| کوئری‌های **بومی** (native SQL) با فیلتر `@TenantId` بازنویسی **نمی‌شوند** | ⚠️ | هر کد آینده با SQL بومی باید شرط `tenant_id` را دستی بگذارد |
| Hibernate فیلتر `@TenantId` را روی کوئری‌های مشتق‌شده و `findById` خودکار اعمال می‌کند | ⚠️ | `TenantIsolationTest` — قید پذیرش فاز ۱ |
| `@PrePersist` در `TenantAwareEntity` پیش از مقدارگذاری `@TenantId` توسط Hibernate اجرا می‌شود | ✅ **تأیید شد** (§۱۰.۴) | مسیر نوشتن Fail-Closed در دامنه پلتفرم |
| `ScopedValue` به ریسمان دیگر (از جمله Virtual Thread) منتقل **نمی‌شود** | ⚠️ | `TenantContextTest` → `CrossThreadBehaviour` |

> **درباره ردیف سوم — این توضیح در §۱۰.۴ اصلاح شد.** نوشته بودیم «ترتیب تضمین‌شده
> نیست، پس تست با **هر دو ترتیب** سبز می‌ماند». اندازه‌گیری واقعی نشان داد ترتیب
> قطعی است: `@PrePersist` **پیش از** مقدارگذاری `@TenantId` اجرا می‌شود، پس
> `requireTenantId()` پرتاب می‌کند و نوشتن در دامنه پلتفرم **رد** می‌شود.
> تست حالا همین را صریح می‌سنجد، نه اینکه نتیجه را بی‌طرف رها کند.

### ۸.۴ تصمیم‌های فاز ۱ که در این سند ثبت می‌شوند

| تصمیم | چرا |
|---|---|
| `StructuredTaskScope` استفاده **نمی‌شود** | در JDK 25 هنوز Preview است (خطای کامپایل واقعی — بخش ۳) |
| جایگزین: `Executors.newVirtualThreadPerTaskExecutor()` + `Future` | نهایی و بدون نیاز به flag پیش‌نمایش |
| دو زنجیره فیلتر امنیتی جدا (Mini App بی‌حالت / ادمین با کوکی و CSRF) | توکن در هدر → CSRF ممکن نیست؛ کوکی → CSRF اجباری |
| `AbstractHttpConfigurer::disable` با lambda نوشته می‌شود | method reference هشدار Null-safety می‌دهد؛ lambda نمی‌دهد |
| `spring.jpa.hibernate.ddl-auto: validate` حتی در تست | اگر نگاشت و Migration اختلاف داشته باشند، باید همان‌جا لو برود |
| جدول کاوشگر جداسازی از راه Migration **فقط-تست** ساخته می‌شود، نه `@Sql` | `ddl-auto: validate` پیش از اجرای `@Sql` اجرا می‌شود و شکست می‌خورد |
| کلاس‌های کمکی تست داخل بسته ماژول `tenant` هستند | Modulith هر زیربسته مستقیم ریشه را ماژول جدا می‌شمارد |

---

## ۹. ثبت نخستین بیلد واقعی

> این بخش با **اجرای واقعی `mvn`** نوشته شده، نه با خواندن کد. جایگزین فرض‌های
> بخش ۸.۲ می‌شود: هر ردیفی که اینجا ✅ یا ❌ گرفته، **آزموده شده است**.

### ۹.۰ پیش‌نیازی که بیلد را متوقف کرد

نخستین اجرا با این خطا شکست خورد:

```text
[ERROR] No toolchain found for type jdk
[ERROR] Cannot find matching toolchain definitions for the following toolchain types: jdk [ version='25' ]
```

**علت واقعی:** فایل `~/.m2/toolchains.xml` وجود نداشت. این همان ردیف ⚠️ بخش ۸.۲ بود
که حالا **از حدس به واقعیت تأییدشده** ارتقا می‌یابد: بدون آن فایل، `mvn` حتی یک خط
کد را هم کامپایل نمی‌کند.

> ⚠️ **دام محیطی که ارزش ثبت دارد:** در Git Bash این ماشین، `$HOME` برابر `/z/` است،
> نه `C:\Users\<کاربر>`. پس نوشتن در `~/.m2/toolchains.xml` فایل را در `Z:\.m2\`
> می‌گذارد و Maven — که از `user.home` جاوای خودش (`C:\Users\o.parishani`) می‌خواند —
> آن را نمی‌بیند. **مسیر درست را صریح بنویسید، نه با `~`.**

### ۹.۱ ❌ فرض‌هایی که بیلد واقعی رد کرد

| ادعای بخش ۸ | وضعیت | حقیقت | اصلاح |
|---|---|---|---|
| `org.hibernate.cfg.MultiTenancyStrategy` وجود دارد | ❌ **رد شد** | در Hibernate 7 **حذف شده است** | کلاس حذف شد |
| `AvailableSettings.MULTI_TENANT` وجود دارد | ❌ **رد شد** | در Hibernate 7 **حذف شده است** | کلاس حذف شد |
| `org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer` | ❌ **رد شد** | بسته در Boot 4 بازچینی شده | → `org.springframework.boot.hibernate.autoconfigure` |

**درس روش‌شناسی — مهم‌ترین نتیجه این بخش:** این سه خطا **در IDE هیچ هشداری نداشتند**.
هر سه به‌عنوان «بدون خطای IDE» در بخش ۸.۱ ثبت شده بودند. یعنی **IDE برای تأیید
درستی امضای API کافی نیست** — فقط کامپایل واقعی حجت است. بخش ۸.۰ از قبل همین
هشدار را داده بود؛ اینجا مصداقش پیدا شد.

### ۹.۲ ✅ چیزهایی که بیلد واقعی تأیید کرد

| ادعا | نحوه تأیید |
|---|---|
| `spring-boot-starter-webmvc` در Boot 4.1.1 **موجود است** | جار `spring-boot-starter-webmvc-4.1.1.jar` در مخزن Maven یافت شد — ردیف ⚠️ بخش ۸.۲ بود |
| Spring Boot 4.1.1 روی Maven Central موجود است | resolve شد |
| کلاس‌های اصلی (۵۷ فایل) کامپایل می‌شوند | `mvn -B clean compile` با `exit=0` |

### ۹.۳ 🔍 API واقعی Hibernate 7 در مورد چند‌مستأجری

آنچه با باز کردن جار واقعی (`hibernate-core-7.2.14.Final.jar`، `javap`) تأیید شد:

| موضوع | Hibernate 6 | Hibernate 7 |
|---|---|---|
| تنظیم استراتژی | `hibernate.multiTenancy=DISCRIMINATOR` | **حذف شده** — کلاسی وجود ندارد |
| کلید تنظیم resolver | `AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER` | `MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER` |
| اینترفیس resolver | `CurrentTenantIdentifierResolver` (خام) | `CurrentTenantIdentifierResolver<T>` (**جنریک**) + متد پیش‌فرض `isRoot(T)` |

**نکته معماری:** حذف `hibernate.multiTenancy` یک **ساده‌سازی** بود، نه پس‌رفت.
آن تنظیم فقط به جداسازی *سطح اتصال* (DATABASE/SCHEMA) مربوط بود؛ اثباتش این است که
متد `MultiTenancy.isMultiTenancyEnabled()` در Hibernate 7 **فقط** وجود یک
`MultiTenantConnectionProvider` را چک می‌کند. جداسازی *سطح سطری* که ما استفاده
می‌کنیم از ابتدا فقط با `@TenantId` + resolver کار می‌کرد و به آن تنظیم نیازی نداشت.

> **قاعده‌ای که این بخش تثبیت می‌کند:** در این پروژه، یک API خارجی تا وقتی با
> **اجرای واقعی** تأیید نشده، ✅ نمی‌گیرد. «IDE خطا نداد» شاهد کافی نیست.

---

## ۱۰. ثبت نخستین اجرای کامل تست‌ها — دروازه فاز ۱

### ۱۰.۰ نتیجه

```text
[INFO] Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

شامل هر سه تست تعیین‌کننده:

| تست | تعداد | چه چیزی را ثابت می‌کند |
|---|---|---|
| `TenantIsolationTest` | ۶ | جداسازی مستأجر روی PostgreSQL واقعی — قید پذیرش فاز ۱ |
| `ApplicationModulesTest` | ۲ | `ApplicationModules.verify()` — مرزهای ماژول سالم است |
| `ArchitectureTest` | ۹ | قواعد ArchUnit |

دستور بازتولید:

```bash
cd backend
TESTCONTAINERS_RYUK_DISABLED=true mvn -B clean verify \
  -Dtest.postgres.image=postgres:16-alpine
```

### ۱۰.۱ ⚠️ دو محدودیت محیطی — نتیجه را مشروط می‌کنند

**۱) تصویر PostgreSQL ۱۷ در دسترس نبود.** هدف Production نسخه ۱۷ است، اما
`docker pull` برای هر تصویری با `403 Forbidden` از `production.cloudfront.docker.com`
رد می‌شود (هم `postgres:17-alpine` و هم `alpine:3.20`). تنها تصاویر موجود روی این
ماشین: `postgres:16`، `postgres:16-alpine`، elasticsearch/kibana ‏۹.۵.۱.

**یعنی این اجرای سبز روی PostgreSQL ۱۶ گرفته شده، نه ۱۷.** اعتبارش تا حدی است که
قابلیت‌های به‌کاررفته — `jsonb`، `pgcrypto`، `pg_trgm` و فیلتر `@TenantId` هایبرنیت —
بین ۱۶ و ۱۷ یکی باشند. برای همین تصویر در `TenantIsolationTest` **پارامتری** شد
(`-Dtest.postgres.image`) و پیش‌فرض همان ۱۷ ماند: تخفیف به‌صورت پیش‌فرض خاموش است،
نه پنهان. **پیش از انتشار، این تست باید روی ۱۷ تکرار شود.**

**۲) `testcontainers/ryuk` قابل دریافت نبود.** Testcontainers پیش از هر کانتینری
اول Ryuk (کانتینر جمع‌کننده زباله) را بالا می‌آورد؛ آن هم از Docker Hub می‌آید و
همان خطای ۴۰۳ را می‌دهد. با `TESTCONTAINERS_RYUK_DISABLED=true` رد می‌شود.

> ⚠️ این متغیر **عمداً در `pom.xml` ثابت نشد.** Ryuk همان چیزی است که کانتینرها را
> پس از تست پاک می‌کند؛ خاموش‌کردن دائمی‌اش روی CI به انباشت کانتینر می‌انجامد.
> راه‌حل درست، رفع دسترسی شبکه است. متغیر فقط برای اجرای محلی است.

### ۱۰.۲ ❌ چهار نقص واقعی که فقط با **اجرای** کانتکست لو رفتند

هیچ‌کدام با `mvn compile` پیدا نمی‌شدند. هر چهار مورد **بعد از** موفقیت کامپایل
و فقط هنگام بالا آمدن کانتکست ظاهر شدند.

| # | نشانه | ریشه واقعی | اصلاح |
|---|---|---|---|
| ۱ | `Schema validation: missing table [audit_logs]` | در Boot 4 کلاس `FlywayAutoConfiguration` از `spring-boot-autoconfigure` به ماژول `spring-boot-flyway` منتقل شده. پروژه `flyway-core` را داشت ولی استارتر را نه — پس **Flyway بی‌صدا هرگز اجرا نمی‌شد** | افزودن `spring-boot-starter-flyway` |
| ۲ | `Schema validation: missing table [event_publication]` | جدول رجیستری رویدادهای Spring Modulith. کتابخانه DDL را در جار نمی‌فرستد و ما هم مهاجرتش نکرده بودیم | مهاجرت `V6` |
| ۳ | `No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'` | Boot 4 پیکربندی خودکار جکسون را به `spring-boot-jackson` برده و آن ماژول **Jackson 3** (`tools.jackson`) را سیم‌کشی می‌کند. کد ما Jackson 2 می‌خواست | مهاجرت سه کلاس به `tools.jackson` |
| ۴ | `No enum constant SerializationFeature.write-dates-as-timestamps` | در Jackson 3 ویژگی‌های تاریخ به `DateTimeFeature` منتقل شده‌اند. کلید قدیمی در `application.yml` **بایند نمی‌شود و کانتکست را می‌شکند** — نه با هشدار | انتقال به `spring.jackson.datatype.datetime` |

**درس روش‌شناسی (تکمیل درس بخش ۹.۱):** سه فرض از چهار فرض بخش ۹ با **کامپایل واقعی**
رد شدند. این چهار مورد از **اجرای واقعی** رد شدند. یعنی سلسله‌مراتب شاهد این است:

```text
IDE بدون خطا   →   کافی نیست      (درس ۹.۱)
mvn compile OK →   کافی نیست      (درس ۱۰.۲)
اجرای کامل تست →   حجت است        (همین بخش)
```

مورد ۱ بدترین‌شان بود چون **بی‌صدا** بود: نه خطای کامپایل، نه هشدار راه‌اندازی.
اگر تستی روی PostgreSQL واقعی وجود نداشت، این نقص تا نخستین استقرار زنده می‌ماند —
جایی که دیگر هزینه‌اش یک مهاجرت نیست، یک دیتابیس بدون اسکیما است.

### ۱۰.۳ ✅ ادعاهای بخش ۸ که این اجرا تأیید کرد

| ادعای بخش ۸ | وضعیت جدید | شاهد |
|---|---|---|
| `hibernate.ddl-auto: validate` با ستون `JSONB` نگاشت‌شده به `String` سازگار است (**پرجمعیت‌ترین ریسک فاز**) | ✅ **تأیید شد** | `audit_logs` سه ستون `jsonb` دارد و کانتکست با `validate` بالا آمد — ردیف ⚠️ بخش ۸.۲ |
| `@Container @ServiceConnection` روی فیلد static کانتینر را پیش از کانتکست بالا می‌آورد | ✅ **تأیید شد** | `TenantIsolationTest` |
| Hibernate فیلتر `@TenantId` را روی کوئری مشتق‌شده و `findById` خودکار اعمال می‌کند | ✅ **تأیید شد** | هر ۶ تست جداسازی؛ به‌ویژه `findByIdFromAnotherTenantReturnsEmpty` |
| سازگاری Spring Modulith 2.1.1 با Boot 4.1.1 | ✅ **تأیید شد** | `ApplicationModulesTest` سبز |
| کوئری‌های بومی با فیلتر `@TenantId` بازنویسی نمی‌شوند | ✅ **تأیید شد** | `findRawTenantIdsByLabel` عمداً همان را دور می‌زند و مقدار خام را می‌خواند |
| `maven-toolchains-plugin` بدون `toolchains.xml` بیلد را متوقف می‌کند | ✅ **تأیید شد** | بخش ۹.۰ |
| `spring-boot-starter-webmvc` در Boot 4.1.1 موجود است | ✅ **تأیید شد** | بخش ۹.۲ |

### ۱۰.۴ 🔍 اندازه‌گیری واقعی ترتیب `@PrePersist` و `@TenantId`

بخش ۸.۳ ردیف سوم را ⚠️ گذاشته بود و در توضیحش نوشته بود «تست با **هر دو ترتیب** سبز
می‌ماند». آن جمله دیگر درست نیست و اصلاح شد.

اندازه‌گیری با یک Probe موقت — تست طوری تغییر کرد که اگر ذخیره در دامنه پلتفرم
استثنا پرتاب **نمی‌کرد**، شکست بخورد:

```text
java.lang.AssertionError: PROBE: platform write THREW ->
  ir.omid.smartteacher.common.error.TenantNotResolvedException
```

**نتیجه قطعی:** Hibernate هنگام persist ستون `@TenantId` را از resolver پر **نمی‌کند**.
پس `@PrePersist` در `TenantAwareEntity` مقدار `null` می‌بیند، به `requireTenantId()`
می‌رسد و پرتاب می‌شود. یعنی نوشتن بدون مستأجر واقعاً **Fail-Closed** است.

> ⚠️ فرض اولیه‌ی من خلاف این بود: استدلال کرده بودم که resolver در دامنه پلتفرم
> `PLATFORM_SENTINEL` برمی‌گرداند، پس ستون پیش از `@PrePersist` پر می‌شود و ذخیره
> **موفق** می‌شود. استدلال از روی کد معتبر به نظر می‌رسید و **غلط بود**. فقط اندازه‌گیری
> نشانش داد. این دقیقاً همان دلیلی است که این سند وجود دارد.

`TenantIsolationTest` حالا این قرارداد را **صریح** می‌سنجد (انتظار
`TenantNotResolvedException` و نبود هیچ رکوردی)، نه اینکه نتیجه را بی‌طرف رها کند.

### ۱۰.۵ ⚠️ کارهای باقی‌ماندهٔ فاز ۱ (صادقانه، نه سبزِ کاذب)

| کار | چرا باز است |
|---|---|
| تکرار `TenantIsolationTest` روی PostgreSQL ۱۷ | تصویر ۱۷ در این محیط قابل دریافت نیست (§۱۰.۱) |
| نوشتن `EitaaSecretKeyDerivationTest` | کار فاز ۲ است. **جهت کلید اما تأیید شد** — با `openssl` مستقل، بخش ۱.۱ |
| اجرای `ApplicationModules.verify()` با `sharedModules` واقعی | فعلاً هیچ ماژول اشتراکی وجود ندارد |

> **این‌ها دروازه فاز ۱ را باز نمی‌گذارند، ولی باید پیش از استقرار بسته شوند.**
> تفاوت را با «سبز بودن» باید حفظ کرد: تست‌ها سبزند، اما سبز بودنشان روی نسخه‌ای
> جز نسخه Production گرفته شده است.


---

## ۱۱. ثبت فاز ۲ — یکپارچه‌سازی ایتا (نیمه Backend)

### ۱۱.۰ نتیجه

```text
[INFO] Tests run: 315, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:34 min
```

دستور بازتولید:

```bash
cd backend
TESTCONTAINERS_RYUK_DISABLED=true mvn -B clean verify \
  -Dtest.postgres.image=postgres:16-alpine
```

> ⚠️ **همان دو قید بخش ۱۰.۱ دست‌نخورده باقی است:** اجرا روی PostgreSQL **۱۶** (نه ۱۷)
> و با `TESTCONTAINERS_RYUK_DISABLED=true`. هیچ‌کدام برداشته نشده‌اند.

> ⚠️ **۳۱۵ تست یعنی این اجرا شامل فاز ۱ هم هست،** نه فقط ۱۸ تست الزامی
> `docs/eitaa.md` بخش ۱۳. آن ۱۸ تست همه سبزند (`§۱۱.۳`).

### ۱۱.۱ 🔴 نخستین اجرای کامل، ۷ شکست داد — و همه‌شان تازه بودند

هر ۷ شکست در **نخستین اجرای واقعی** تست‌هایی بودند که پیش‌تر نوشته شده بودند.
هیچ‌کدام پیش از این اجرا نشده بود، پس هیچ‌کدام «رگرسیون» نبود — همه **نقص‌های
تازه‌کشف‌شده** بودند. این تکرار درس بخش ۱۰.۲ است: کد نوشته‌شده حجت نیست.

| # | تست | ریشه واقعی | نوع |
|---|---|---|---|
| ۱–۴ | `EitaaStartParamInjectionTest` (چهار آزمون) | `start_param` هرگز URL-decode نمی‌شد | 🔴 **نقص Production** |
| ۵ | `EitaaInitDataValidationTest$Acceptance.reportsAuthDate` | آزمون دقت زیرثانیه انتظار داشت؛ `auth_date` یونیکس‌ثانیه است | نقص آزمون |
| ۶ | `EitaaInitDataReplayedTest.guardAllowsReuseAfterWindowExpires` | مرز «منقضی» یک لحظه خطا داشت | 🔴 **نقص Production** |
| ۷ | `BotTokenNeverLoggedTest.successfulRealPathsLeakNothing` | مسیرهای موفق هیچ لاگی تولید نمی‌کنند، پس نگهبان «آزمون بی‌معنا نباشد» افتاد | نقص آزمون |

### ۱۱.۲ 🔍 دو نقص واقعی کد Production که این اجرا لو داد

**۱) `start_param` رمزگشایی نمی‌شد.** مسیر Deep Link مقدار
`..%2F..%2F..%2Fetc%2Fpasswd` را دقیقاً به همان شکل تحویل می‌داد. `inert` بود
(هیچ‌کجا اجرا نمی‌شد) ولی **غلط** بود: `%26` و `%3D` و `%2F` هرگز با هیچ
شناسه‌ای جور نمی‌شوند، پس در فاز ۹ یک Deep Link **بی‌صدا** کار نمی‌کرد — بدون
خطا، بدون لاگ، بدون سرنخ.

اصلاح: `parseStartParam` یک‌بار رمزگشایی می‌کند، **پس از** بررسی امضا (چون
رمزگشایی پیش از امضا `data_check_string` را با آنچه ایتا امضا کرده ناهمخوان
می‌کند — همان دلیلی که `parseQuery` رمزگشایی نمی‌کند).

> ⚠️ **و یک تفاوت عمدی با `user`:** خطای رمزگشایی در `user` کل ورود را رد
> می‌کند (هویت را حمل می‌کند)، ولی در `start_param` مقدار خام نگه داشته و فقط
> WARN ثبت می‌شود. خراب بودن یک میان‌بر Deep Link نباید جلوی ورود معلم را بگیرد.
> این عدم‌تقارن در Javadoc همان متد نوشته شده تا کسی آن را باگ نپندارد.

**۲) مرز «منقضی‌شده» در `InMemoryReplayGuard` یک لحظه خطا داشت.** مقدار ذخیره‌شده
خودِ لحظه انقضا است، ولی شرط `previous.isBefore(Instant.now())` لحظه دقیق انقضا را
«هنوز تازه» حساب می‌کرد. با پنجره پنج‌دقیقه‌ایِ Production این خطا **نامرئی** است؛
آزمون با پنجره‌ای در حد صفر روی ساعتی با دقت کم آن را بیرون کشید.

اصلاح: `!previous.isAfter(Instant.now())` در `markUsed`، و همان مرز در
`purgeExpired` تا دو جای کد یک تعریف از «منقضی» داشته باشند.

> ⚠️ این را به‌عنوان «آزمون شکننده بود، سستش کردم» ثبت نمی‌کنم. مرز **غلط** بود و
> اصلاح شد؛ درستی آزمون هم عارضیِ همان اصلاح است.

**۳) شکست ارسال، تراکنش را می‌کشت** — این یکی را **اجرای کامل** پیدا نکرد؛ نوشتن
آزمونِ `SendFailureDoesNotRollbackAssignmentTest` پیدا کرد و اصلاح **پیش از** اجرای
کامل انجام شد. چون تغییری در قرارداد Production است، اینجا ثبت می‌شود:

لایه HTTP شکست ایتا (`ok:false`، کد ۵xx، بدنه بدشکل) را با **استثنا** علامت می‌زد.
هر فراخوانی که در یک تراکنش انجام می‌شد — مثل انتشار تکلیف — با یک ارسال ناموفق
rollback می‌شد و **کار معلم از بین می‌رفت**، در حالی که کار او هیچ ایرادی نداشت.
`docs/eitaa.md` بخش ۹ صریح می‌گوید «تکلیف در سیستم ما ذخیره شده؛ ایتا فقط یک کانال
توزیع است.»

اصلاح: سه فراخوان (متن، فایل، خصوصی) استثنای لایه HTTP را در **مقدار**
`EitaaSendResult` تبدیل می‌کنند. مرز اصلاح عمداً باریک است — «قابلیت وجود ندارد»
(`EitaaCapabilityNotSupportedException`) همچنان استثنا می‌ماند، چون آن «الآن نشد»
نیست؛ «ممکن نیست» است و یعنی برنامه باید عوض شود. آزمون
`capabilityFailureIsStillAnExceptionAndRollsBack` همین مرز را می‌بندد.

### ۱۱.۳ ✅ ۱۸ تست الزامی `docs/eitaa.md` بخش ۱۳

| تست | تعداد | وضعیت |
|---|---|---|
| `EitaaSecretKeyDerivationTest` | — | ✅ سبز |
| `EitaaInitDataValidationTest` | ۱۱ | ✅ سبز |
| `EitaaInitDataTamperedTest` | — | ✅ سبز |
| `EitaaInitDataExpiredTest` | — | ✅ سبز |
| `EitaaInitDataReplayedTest` | ۱۰ | ✅ سبز |
| `EitaaInitDataUrlEncodingTest` | ۷ | ✅ سبز |
| `EitaaConstantTimeComparisonTest` | — | ✅ سبز |
| `EitaaUserIdIsLongTest` | — | ✅ سبز |
| `EitaaOkFlagAlwaysCheckedTest` | — | ✅ سبز |
| `MockSendToChatTest` | — | ✅ سبز |
| `MockSendToUserTest` | — | ✅ سبز |
| `ScheduledSendUsesDateParamTest` | — | ✅ سبز |
| `CapabilityNotSupportedTest` | — | ✅ سبز |
| `MessageSplitLongTextTest` | ۱۹ | ✅ سبز |
| `SendFailureDoesNotRollbackAssignmentTest` | ۷ | ✅ سبز |
| `BotTokenNeverLoggedTest` | ۹ | ✅ سبز |
| `StartParamInjectionTest` | ۱۱ | ✅ سبز |
| `ArchUnit_noWebhookControllerTest` | ۵ | ✅ سبز |

### ۱۱.۴ 🔍 قراردادی که آزمون «شکست ارسال، تکلیف را باطل نکند» تثبیت کرد

`SendFailureDoesNotRollbackAssignmentTest` مکانیزم را می‌سنجد، چون «تکلیف» فاز ۷ است:
یک نوشتن واقعی چندجدولی (`EitaaAccountProvisioner.findOrCreate`) در **همان
تراکنشی** که ارسال ایتا شکست می‌خورد، روی PostgreSQL واقعی.

سه آزمون در دو جهت، تا نتیجه نتواند **بی‌معنا** سبز شود:

| آزمون | چه چیزی را می‌بندد |
|---|---|
| `failedSendDoesNotRollBackTheSavedWork` | ادعای اصلی |
| `theEscapingExceptionWouldHaveRolledBackTheSameWork` | ثابت می‌کند همان کار، **اگر** استثنا بیرون بزند، از بین می‌رود — پس آزمون اول چیزی را محافظت می‌کند |
| `rollbackActuallyRemovesRows` | ثابت می‌کند این دیتابیس واقعاً rollback می‌کند |
| `capabilityFailureIsStillAnExceptionAndRollsBack` | مرز اصلاح: «ممکن نیست» با «الآن نشد» یکی نیست |

### ۱۱.۵ ⚠️ آزمون‌های `@SpringBootTest` و پیام `kill self fork JVM`

در پایان کلاس‌های `@SpringBootTest` این خط در لاگ می‌آید:

```text
[ERROR] Surefire is going to kill self fork JVM.
The exit has elapsed 30 seconds after System.exit(0).
```

**اندازه‌گیری جداسازی:** `TenantIsolationTest` به‌تنهایی اجرا شد:

```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 22.63 s
[ERROR] Surefire is going to kill self fork JVM. ...
[INFO] Total time:  01:02 min
```

۶ تست سبز، و **۳۰ ثانیه** از آن ۶۲ ثانیه، انتظار اجباری پیش از کشتن fork است.

**نتیجه:** این پیام از یک ریسه غیر-daemon می‌آید که با **بالا آمدن کانتکست
`@SpringBootTest` + Testcontainers** ساخته می‌شود — نه از هیچ‌کدام از تست‌های فاز ۲.
تنها کلاسی که هر دو اجرا (فاز ۱ و فاز ۲) در آن مشترک است، همین یکی است.

> ⚠️ **آنچه نمی‌توانم بگویم:** «در دروازه فاز ۱ هم دیده می‌شد.» لاگ آن اجرا نگه داشته
> نشده و تنها سند باقی‌مانده روی دیسک (`backend/build.log`) یک اجرای **ناموفق** است که
> در آن `TenantIsolationTest` با `Errors: 1` افتاد و کانتکست هرگز بالا نیامد — پس
> امکان دیده‌شدن این پیام در آن اجرا وجود نداشت. پس ادعا به همان اندازه است که
> شاهد دارم: **مشترک با فاز ۱ هست، ولی در لاگ فاز ۱ ثبت نشده.**

> ⚠️ **به‌عنوان «مشکل خودمان» ثبت نمی‌کنم و به‌عنوان «مشکل ایتا» هم نه.** در کد
> خودمان `EitaaHttpClient` ریسه‌های virtual را daemon می‌سازد و `@PreDestroy`
> آن‌ها را می‌بندد. منشأ دقیق ریسه **تثبیت نشده** است و تا تثبیت نشدن، همان‌طور
> ثبت می‌شود: مشاهده‌شده، بی‌اثر بر نتیجه، بی‌منشأ.

| اثر | مقدار |
|---|---|
| بر درستی نتیجه | **هیچ** — `BUILD SUCCESS` است |
| بر زمان اجرا | ۳۰ ثانیه در هر اجرایی که کانتکست بالا بیاید |

### ۱۱.۶ ⚠️ کارهای باقی‌ماندهٔ فاز ۲ (صادقانه)

دروازهٔ **نیمه Backend** فاز ۲ گرفته شد. فاز ۲ **کامل نیست** — طبق
`docs/roadmap.md` سه دسته کار باز است:

| دسته | مورد | چرا باز است |
|---|---|---|
| Mini App | گام‌های ۷ تا ۱۰ roadmap: اسکلت، گارد پلتفرم، پنج تب، Deep Link، `requestWriteAccess` | پیاده نشده |
| توکن واقعی | نام `sendFile`/`sendDocument`، نرخ، طول پیام، حجم فایل، منطقه زمانی `date`، دسکتاپ ویندوز | بدون دسترسی به سرویس واقعی ایتا **قابل تأیید نیست**؛ هیچ‌کدام حدس زده نشد و همه در `EitaaProperties` قابل تنظیم‌اند |
| استقرار | تکرار `TenantIsolationTest` روی PostgreSQL ۱۷ | محدودیت محیطی §۱۰.۱ |

> **قاعده‌ای که رعایت شد:** هیچ‌کدام از موارد «توکن واقعی» با یک مقدار حدسی پر نشد.
> هر کدام یک مقدار **محافظه‌کارانه** و **قابل تنظیم** گرفته است، و همان مقدار در
> `EitaaProperties` با صراحت «تأییدنشده» علامت خورده.

---

## ۱۲. ثبت فاز ۲ — نیمه Mini App (گام‌های ۷ تا ۱۰)

### ۱۲.۰ نتیجه دروازه

```text
tsc -b                 ✓  هر سه پروژه (app، node، e2e)
vite build             ✓  1974 module · index-DzS0wbum.js 363.19 kB → 114.63 kB gzip
verify-bundle.mjs      ✓  ✅ باندل پاک است
vitest run             ✓  10 فایل · 117 تست
playwright test        ✓  17 تست (جریان E2E الزامی + پوشش دسترس‌پذیری و رفتار)
eslint .               ✓  بدون خطا
size-limit             ✓  113.28 kB gzip از بودجه ۱۵۰ kB
```

دستور بازتولید:

```bash
cd miniapp
npm ci
npm run build && npm run test && npm run lint && npm run size-check && npm run e2e
```

> ⚠️ **پیش‌نیاز E2E: مرورگر روی سیستم.** بخش ۱۲.۵ را ببینید — دانلود Chromium روی این
> شبکه ممکن نیست و آزمون به Chrome نصب‌شده روی سیستم وصل می‌شود.

### ۱۲.۱ 🔴 چهار نقص واقعی Production که آزمون‌ها لو دادند

هیچ‌کدام از این چهار مورد با بازبینی چشمی پیدا نشد. سه‌تای اول را **اجرای واقعی در
مرورگر** پیدا کرد و چهارمی را آزمونی که حاضر نشد سست شود.

**۱) بزرگ‌نمایی در مرورگر کامل بسته بود.** `index.html` مقدار
`maximum-scale=1.0, user-scalable=no` داشت — الگویی که قالب‌های برنامک می‌گذارند تا
دوبار-ضربه زوم نکند. نتیجه‌اش این بود که کاربر کم‌بینا **نمی‌توانست متن را بزرگ کند**؛
تخلف WCAG 1.4.4.

> ⚠️ این را `axe` (قاعده `meta-viewport`) پیدا کرد، در نخستین اجرای `e2e/a11y.spec.ts`.
> هر ۸ آزمون دسترس‌پذیری روی همین یک مورد می‌افتادند.

اصلاح: هر دو مقدار حذف شدند؛ `viewport-fit=cover` ماند، چون نوار وضعیت به آن نیاز دارد.

**۲) عنوان کارت خطا نسبت کنتراست کافی نداشت.** `--color-danger` مقدار
`hsl(0 84% 60%)` است که روی زمینه روشن حدود **۳.۸:۱** می‌دهد — زیر آستانه ۴.۵:۱ سطح AA.

> 🔴 **این نقص پشت نقص اول پنهان شده بود.** تا وقتی بزرگ‌نمایی بسته بود، `axe` روی
> همان یک تخلف می‌ایستاد و به بقیه نمی‌رسید. اگر فقط یکی را اصلاح کرده بودیم،
> گمان می‌کردیم صفحه تمیز است.

اصلاح: یک توکن جدا برای **متنِ** قرمز اضافه شد — `--color-danger-strong` — چون یک رنگ
پرکننده می‌تواند روشن باشد ولی وقتی خودش رنگ متن شود باید تیره‌تر شود. مقدار روشن
(~۴.۸:۱ روی سفید) در تم روشن، و در تم تاریک مقدار روشن‌ترِ اصلی (~۵.۳:۱).

> ⚠️ این یک **انحراف عمدی از پالت `docs/mini-app.md` بخش ۹** است و همان‌جا ثبت شد.
> پالت سند برای «قرمز» یک مقدار داشت و همان مقدار، وقتی به‌عنوان متن به کار برود،
> در دسترس‌پذیری رد می‌شود. رنگ پرکننده دست‌نخورده ماند.

**۳) 🔴 `detectTheme` نوشته شده بود ولی هیچ‌جا صدا زده نمی‌شد.** صفحه ورود به‌جای آن
`useUiStore.getState().theme` را می‌خواند که همیشه `light` است. یعنی مقدار
`eitaa.colorScheme()` — که سند بخش ۹ «منبع حقیقت تم» می‌نامدش — **کاملاً نادیده
می‌ماند** و برنامه همیشه روشن می‌ماند.

> 🔴 **چرا تا حالا دیده نشد؟** چون `detectTheme` جداگانه و درست آزموده می‌شد. آنچه
> آزموده نشده بود این بود که **کسی صدایش می‌زند یا نه**. آزمون نوشتن برای یک تابع،
> ثابت نمی‌کند آن تابع در مسیر واقعی برنامه است.

اصلاح: `setTheme(detectTheme())` در همان افکت، به‌علاوه آزمون رگرسیون در
`LoginGate.test.tsx` که تم را از SDK جعلی می‌سنجد — نه `detectTheme` را جدا.

**۴) 🔴 تلاش دوباره پس از تازه‌سازی توکن، توکن کهنه را می‌فرستاد.** آزمون
`api-client` انتظار `Bearer access-2` داشت و `Bearer access-1` می‌گرفت.

> 🔴 مسیر «۴۰۱ خورد، رفرش کن، دوباره بفرست» توکن را از حال داخلیِ پل می‌خواند، نه از
> مقداری که `refresh()` همین الان برگردانده بود. یعنی درخواست دوم با توکن منقضی
> دوباره می‌رفت و باز ۴۰۱ می‌خورد. روی کاربر نهایی یعنی «گاهی لاگین می‌پرد».

اصلاح: **کد اصلاح شد، نه آزمون.** آن خط به یک قرارداد نانوشته تکیه داشت («پیاده‌سازی
پل هنگام `refresh()` مقدار `getAccessToken()` را هم عوض می‌کند»)؛ حالا توکن صریحاً
پاس داده می‌شود و قرارداد نانوشته حذف شده است.

### ۱۲.۲ 🕵️ داستان «نشت `dev-data`» — یک مثبت کاذب، و آنچه جایش را گرفت

در همین فاز گمان شد Vite مقدار `VITE_DEV_INIT_DATA` را از `.env.local` به باندل
Production می‌نویسد. **این گمان غلط بود**، و مسیر رسیدن به این نتیجه ارزش ثبت دارد،
چون دو اشتباه پشت سر هم بود:

| مرحله | چه اتفاقی افتاد |
|---|---|
| ۱ | `Select-String "dev-data"` در باندل «MATCH» داد → نتیجه‌گیری: نشت داریم |
| ۲ | 🔴 الگو **زیررشته** بود. رشته‌های دائمی و بی‌ضرر هم آن را داشتند: کلید `'dev-data-missing'` و پیام فارسی‌ای که نام متغیر را توضیح می‌دهد |
| ۳ | یک پلاگین پیکربندی (`stripDevInitData`) نوشته شد تا مقدار را حذف کند |
| ۴ | آزمون درستیِ خودِ پلاگین نشان داد با **برداشتنش** هم مقداری در باندل نیست |

**اندازه‌گیری‌های مرحله ۴** (هر سه، با پاک کردن `dist` و کش):

| حالت | پلاگین | minify | نتیجه |
|---|---|---|---|
| الف | خاموش | روشن | مقدار در باندل **نبود** |
| ب | خاموش | خاموش + بازآرایی عمداً شکننده | مقدار در باندل **نبود** |
| ج | روشن | روشن | باندل **بایت‌به‌بایت یکسان** با حالت الف |

> ⚠️ حالت «ب» مهم‌ترین است: مقدار عمداً به یک ثابت سطح ماژول برده شد تا تاش‌پذیری از
> بین برود و معلوم شود حذف، کار minifier است یا کار bundler. با `--minify false` هم
> چیزی نبود.

**ریشه واقعی:** در `getInitData` شرط `import.meta.env.PROD` **اول** می‌آید. Vite آن را
به `true` تبدیل می‌کند و bundler کل شاخه توسعه را به‌عنوان کد مرده کنار می‌گذارد. در
باندل نهایی، تابع فقط این است:

```js
function getInitData() {
  devDataInUse = false;
  const fromEitaa = eitaa.initData();
  if (fromEitaa) return fromEitaa;
  throw new InitDataUnavailableError('not-in-eitaa', INIT_DATA_MESSAGES['not-in-eitaa']);
}
```

پس **دروازه در کد بسته است، نه در پیکربندی** — و پلاگین حذف شد، چون در حالت «ج» ثابت
شد هیچ کاری نمی‌کند.

> 🔴 دلیل حذفش فقط بی‌فایده بودن نبود: اگر روزی مقدار واقعاً به مسیر Production
> می‌رسید، آن پلاگین مقدار را **بی‌صدا** به `undefined` تبدیل می‌کرد و بیلد سبز
> می‌ماند — یعنی یک نقص واقعی را پنهان می‌کرد. شکست بلند و واضح، رفتار درست است.

**آنچه جایش را گرفت:** `scripts/verify-bundle.mjs`، که در زنجیره `npm run build` است
و خروجی نهایی را می‌خواند.

> 🔴 **خودِ این دروازه هم آزموده شد.** یک «قناری» (`leak-canary-9f3a71`) در یک فایل
> محیطی و به‌صورت رشته نقل‌قول‌شده در باندل کاشته شد؛ دروازه شکست خورد، همان مقدار را
> نام برد، و کد خروج ۱ داد. دروازه‌ای که هرگز شکست نخورده، معلوم نیست کار می‌کند یا نه.

> ⚠️ **و یک درس روی الگو:** نسخه اول `verify-bundle.mjs` هم دنبال زیررشته می‌گشت و
> همان مثبت کاذب را داد. الگویش باریک شد به «مقدار، به‌صورت رشته نقل‌قول‌شده» و دلیلش
> داخل خود فایل نوشته شد تا کسی دوباره باریکش نکند. دروازه‌ای که مثبت کاذب می‌دهد،
> دیر یا زود خاموش می‌شود و دیگر هیچ‌چیز را نمی‌گیرد.

### ۱۲.۳ 🧪 دو نقص آزمون (نه نقص کد)

| آزمون | ریشه |
|---|---|
| `auth.store.test` — انتظار نشانی کامل از `VITE_API_URL` | در محیط آزمون مقداردهی نشده؛ ادعا به **پسوند** مسیر تغییر کرد |
| `api-client.test` — `Unhandled Rejection NETWORK_TIMEOUT` | گیرنده `.rejects` **پس از** جلو بردن تایمرهای جعلی وصل می‌شد |

هر دو مورد نقص آزمون بودند و اصلاحشان سست‌کردن ادعا نبود — یکی ادعا را به چیزی که
واقعاً قابل سنجش است منتقل کرد و دیگری ترتیب را درست کرد.

> ⚠️ و برعکسش هم پیش آمد: آزمون `api-client` که `Bearer access-2` می‌خواست و
> `access-1` می‌گرفت، **نقص آزمون نبود** — همان نقص Production بند ۱۲.۱ مورد ۴ بود.
> وسوسه‌ی «عدد را با واقعیت هم‌خوان کن» اینجا درست نبود.

### ۱۲.۴ ⚠️ دو مورد در سلامت زنجیره ساخت

**۱) هشدار همیشه‌روشنِ حجم باندل.** `chunkSizeWarningLimit` روی ۱۵۰ گذاشته شده بود،
یعنی همان بودجه‌ی مستند که **gzip** است، ولی Vite این عدد را با حجم **خام** مقایسه
می‌کند (۳۶۳ kB). پس هشدار در **هر** بیلد روشن بود.

> ⚠️ هشداری که همیشه روشن باشد، دیر یا زود نادیده گرفته می‌شود — و آن‌وقت هشدار واقعی
> را هم با خودش می‌برد.

اصلاح: عدد به ۴۰۰ kB خام رسید (≈۱۲۶ kB gzip) تا همچنان *پیش از* بودجه هشدار بدهد.
دروازه واقعی `size-check` است.

**۲) دوازده هشدار امنیتی، همه در زنجیره توسعه.** `npm install` دوازده هشدار با شدت
بالا گزارش کرد. **همه‌شان** در وابستگی توسعه بودند:

```text
@size-limit/preset-app → @size-limit/time → estimo → puppeteer-core
  → @puppeteer/browsers → proxy-agent → pac-proxy-agent → get-uri
  → basic-ftp / extract-zip
estimo → nanoid
```

**مسیر اصلاح امن وجود نداشت:** تنها راه `npm audit fix --force` بود که
`@size-limit/preset-app` را به نسخه ۱۱ **پس‌گردانی** می‌کرد — یعنی پایین آوردن همان
ابزاری که بودجه باندل را کنترل می‌کند.

**اصلاح درست، عوض کردن ابزار بود.** `@size-limit/preset-app` برای گزارش «زمان
بارگذاری و زمان اجرا» یک زنجیره کامل Chrome بی‌سر همراه می‌آورد — چیزی که ما
دروازه‌مان را روی آن نمی‌بندیم. بودجه ما **حجم gzip** است و `@size-limit/file` همان را
بی‌واسطه حساب می‌کند.

| قبل | بعد |
|---|---|
| `@size-limit/preset-app` | `@size-limit/file` |
| ۱۲ هشدار شدت بالا | **۰ هشدار** (`found 0 vulnerabilities`) |
| ۱۱۳.۲۸ kB gzip | **۱۱۳.۲۸ kB gzip** — عدد تغییر نکرد |

> ⚠️ هیچ‌کدام از این ۱۲ مورد به باندل ارسالی راه نداشتند (وابستگی توسعه بودند)، پس
> این «رفع یک نقص امنیتی در محصول» نیست — کاهش سطح حمله زنجیره ساخت است.

### ۱۲.۵ ⚠️ محدودیت‌های این دروازه (صادقانه)

**۱) بک‌اند در E2E جایگزین (Mock) می‌شود.** `page.route` روی `**/api/v1/**` پاسخ
می‌دهد. پس این آزمون‌ها **قرارداد را** می‌سنجند، نه سرور واقعی را. «ورود از Mini App
داخل ایتا» با یک نشست واقعی ایتا همچنان در گام ۱۱ فاز ۲ آزموده نشده است.

**۲) 🔴 مرورگر از روی سیستم می‌آید، نه از یک نسخه ثابت.** دانلود Chromium شکست خورد:

```text
Error: Failed to download Chrome for Testing … code: 'UNABLE_TO_VERIFY_LEAF_SIGNATURE'
```

(گواهی پروکسی سازمانی.) راه‌حل: `channel: 'chrome'` که به Chrome نصب‌شده وصل می‌شود.
**هزینه‌اش:** نسخه مرورگر روی هر دستگاه می‌تواند فرق کند، و در CI این راه‌حل ممکن است
کار نکند — آنجا باید Chromium دانلود شود (پس این یک راه‌حل محیطی است، نه تصمیم
معماری).

**۳) `requestWriteAccess` روی دسکتاپ ویندوز همچنان تأییدنشده است.** رفتارش در
`src/eitaa/write-access.ts` با یک **مهلت** پوشش داده شده تا اگر کال‌بک هرگز صدا زده
نشد، صفحه ورود بی‌صدا قفل نشود. ولی این «تاب‌آوری» است، نه «تأیید رفتار».

**۴) `axe` همه‌چیز را نمی‌گیرد.** این ابزار کنتراست، برچسب و ساختار را می‌گیرد؛
«آیا این متن مفهوم است» را نمی‌گیرد. سبز بودن یعنی «مانع ماشین‌خوان نیست».

**۵) تم تاریک فقط در دو صفحهٔ نمونه اسکن شد**، نه همه‌ی مسیرها. اگر صفحه‌ای در فازهای
بعد رنگ اختصاصی بیاورد، باید به `PAGES` در `e2e/a11y.spec.ts` اضافه شود.

### ۱۲.۶ ⚠️ کارهای باقی‌ماندهٔ فاز ۲ (پس از این ثبت)

| دسته | مورد | چرا باز است |
|---|---|---|
| گام ۱۱ | `sendFile`/`sendDocument`، نرخ، طول پیام، حجم فایل، منطقه زمانی `date`، دسکتاپ ویندوز | **بدون توکن واقعی ایتا قابل تأیید نیست.** هیچ‌کدام حدس زده نشد |
| پذیرش | «کاربر جدید، مستأجر شخصی و اشتراک رایگان می‌گیرد» | زیرساختش هست؛ اتصال به اشتراک فاز ۱۰ |
| پذیرش | «ارسال واقعی به یک گروه ایتا با توکن واقعی» | نیازمند توکن واقعی |
| استقرار | تکرار `TenantIsolationTest` روی PostgreSQL ۱۷ | محدودیت محیطی — بخش ۱۰.۱ |
| Mini App | E2E جریان‌های ۲ تا ۸ مستند `docs/mini-app.md` بخش ۱۲ | صفحه‌هایشان در فازهای ۵ تا ۱۰ ساخته می‌شوند |
| Mini App | `requestWriteAccess` در رابط کاربری | ماژول و آزمونش هست؛ صفحه‌ای که صدایش بزند فاز ۷ است |

> **قاعده‌ای که رعایت شد:** موارد «توکن واقعی» با هیچ مقدار حدسی پر نشدند. هر کدام
> یک مقدار محافظه‌کارانه و قابل تنظیم در `EitaaProperties` گرفته‌اند و همان‌جا
> «تأییدنشده» علامت خورده‌اند.

## ۱۳. ثبت فاز ۳ — درس‌ها و کلاس‌ها (گام ۱ از ۳)

### ۱۳.۰ نتیجه

```text
mvn -B clean verify -Dtest.postgres.image=postgres:16-alpine

Tests run: 359, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — 1:35 min
```

سه تست تازه و یک تست دیتابیسی:

| کلاس | تعداد | نوع |
|---|---|---|
| `ClassRoomServiceAccessTest` | ۹ | unit — بدون Docker |
| `SearchPatternTest` | ۱۲ | unit |
| `PageableFactoryTest` | ۱۳ | unit |
| `ClassRoomTenantIsolationTest` | ۱۰ | `@SpringBootTest` + Testcontainers |

`ApplicationModulesTest.verify()` و هر ۹ قاعده `ArchitectureTest` هم با ماژول
تازه سبز ماندند — یعنی مرزهای ماژولار و قواعد ArchUnit با `classroom` نشکستند.

### ۱۳.۱ 🔴 نقص واقعی که این دروازه لو داد: ایندکس بی‌مصرف

**آنچه V8 ساخته بود:**

```sql
CREATE INDEX idx_classrooms_name_trgm ON classrooms USING gin (name gin_trgm_ops);
```

**آنچه سرویس می‌زند:**

```sql
where lower(c.name) like :pattern escape '\'
```

این دو به هم نمی‌خورند. ایندکس روی تری‌گرام‌های `name` است و کوئری تری‌گرام‌های
`lower(name)` را می‌خواهد؛ پستگرس نمی‌تواند این دو را به هم نگاشت کند.

**حدس نزدم — با `EXPLAIN` سنجیدم.** روی جدولی با ۲۰٬۰۰۰ ردیف، و با
`SET enable_seqscan = off` (یعنی وقتی پلنر را به زور به سمت ایندکس هل می‌دهیم):

```text
 Seq Scan on c
   Filter: (lower((name)::text) ~~ '%پنجم%'::text)
```

با ایندکس روی `lower(name)` پلن عوض شد:

```text
 Bitmap Heap Scan on c
   Recheck Cond: ((lower((name)::text) ~~ '%پنجم%'::text) AND (deleted_at IS NULL))
   ->  Bitmap Index Scan on idx_classrooms_name_trgm
```

**چرا این مهم است و «فقط یک بهینه‌سازی» نیست؟** ایندکس بی‌مصرف بی‌اثر نیست:
هر `INSERT` و `UPDATE` باید درختش را هم به‌روز کند و هر حذف نرم هم دستش می‌زند.
یعنی هزینه دائمی نوشتن می‌دهیم و در عوضش هیچ نمی‌گیریم. بدتر، هر خواننده‌ای که
V8 را ببیند فکر می‌کند جست‌وجو ایندکس دارد و دیگر سراغش نمی‌رود.

**اصلاح در `V9__fix_classroom_search_index.sql`، نه با ویرایش V8.** چون V8 در
دیتابیس توسعه (`docker-compose`، `localhost:5432/smart_teacher`) اعمال شده بود و
ویرایشش `checksum` را می‌شکست — همان چیزی که قاعده ۱ بخش ۱۲ `database.md` از آن
نهی کرده. V9 همان ایندکس را `DROP` و شکل درستش را می‌سازد.

**قفل شدنش:** تست `searchIndexMatchesTheQueryExpression` روی `pg_indexes` کوئری
می‌زند و بررسی می‌کند که تعریف ایندکس `lower(` دارد. یک ایندکس بی‌مصرف هیچ خطایی
نمی‌دهد، پس بدون این تست بازگشتش دوباره نامرئی می‌شد.

> **درس این مورد:** نه کامپایلر، نه ArchUnit، نه هیچ تست لایه‌ای نمی‌تواند بگوید
> «این ایندکس استفاده نمی‌شود». فقط `EXPLAIN` روی دیتابیس واقعی می‌گوید.

### ۱۳.۲ ✅ ادعاهایی که این اجرا تأیید کرد

| ادعا | چگونه تأیید شد |
|---|---|
| Migration و نگاشت با هم می‌خوانند | کانتکست با `ddl-auto: validate` بالا آمد؛ V8 و V9 اعمال شدند |
| فیلتر خودکار `@TenantId` روی جدول‌های تازه کار می‌کند | `classRoomOfAnotherTenantIsInvisible` — هم از مخزن، هم از سرویس |
| کلید خارجی ترکیبی جلوی ارجاع بین‌مستأجری را می‌گیرد | `compositeForeignKeyBlocksCrossTenantSubjectLink` — `INSERT` خام که لایه سرویس را دور می‌زند، `DataIntegrityViolationException` گرفت |
| یکتایی کد درس داخل مستأجر و بی‌حساسیت به بزرگی حروف | `subjectCodeIsUniquePerTenantCaseInsensitively` |
| رد دسترسی معلم به کلاس همکار ثبت می‌شود | `select count(*) from audit_logs where action='SECURITY_ACCESS_DENIED'` برابر ۱ |
| محدودیت معلم در **شمارش** هم اعمال می‌شود، نه فقط در محتوا | `asTeacherA.totalElements()` برابر ۱ |
| `ON DELETE SET NULL (subject_id)` روی پستگرس ۱۶ کار می‌کند | مایگریشن بدون خطا اعمال شد (این نگارش ستونی از پستگرس ۱۵ آمده) |

### ۱۳.۳ ⚠️ محدودیت محیطی — نتیجه را مشروط می‌کند

**تست‌های دیتابیسی روی `postgres:16-alpine` اجرا شدند، نه `postgres:17-alpine`.**

تصویر پیش‌فرض در کد `postgres:17-alpine` است — همان نسخه Production — ولی
Docker Hub برای این محیط ۴۰۳ می‌دهد و ۱۷ در دسترس نیست. پس هر بار با
`-Dtest.postgres.image=postgres:16-alpine` اجرا شد.

⇒ **پیش از استقرار، `TenantIsolationTest` و `ClassRoomTenantIsolationTest` باید
روی PostgreSQL ۱۷ تکرار شوند.** تا آن روز، «سبز» یعنی «روی ۱۶ سبز».

⚠️ نکته‌ای که این محدودیت مهم می‌کند: مایگریشن V8 از `ON DELETE SET NULL (subject_id)`
استفاده می‌کند که نگارش ستونی است و از پستگرس ۱۵ آمده. روی ۱۶ کار می‌کند ولی
تأیید نهایی روی ۱۷ انجام نشده.

### ۱۳.۴ ⚠️ کارهای باقی‌ماندهٔ گام ۱ فاز ۳

| مورد | چرا باز است |
|---|---|
| `students`، `class_students`، `/classes/{id}/students`، `/classes/{id}/dashboard` | ✅ گام ۲ — بخش ۱۴ |
| صفحه‌های Mini App برای کلاس‌ها و دانش‌آموزان | گام ۳ همین فاز |
| بازتکرار تست‌های دیتابیسی روی PostgreSQL ۱۷ | محدودیت محیطی — بخش ۱۳.۳ |
| انتقال کلاس بین معلمان | نیازمند فهرست اعضای مستأجر؛ فاز ۴ (پنل مدیریت) |

### ۱۳.۵ 🔍 تصمیم‌هایی که این فاز ثبت می‌کند

**۱) `homeroomTeacherId` از بدنه درخواست خوانده نمی‌شود.** نه در ساخت و نه در
ویرایش. اگر می‌پذیرفتیم، معلمی می‌توانست شناسه کاربری از مستأجر دیگر را بفرستد و
آن کاربر — بدون آنکه بداند — این کلاس را در فهرست خودش می‌دید، چون فهرست بر اساس
همین ستون فیلتر می‌شود. بررسی عضویت، نیازمند فهرست اعضای مستأجر است که فاز ۴ می‌آورد.

**۲) `/subjects` افزوده‌ای آگاهانه به فهرست `api.md` است، نه فراموشی.** ERD جدول
`subjects` را الزام کرده و `classrooms.subject_id` بدون راهی برای ساختن درس عملاً
بی‌استفاده می‌ماند. در `api.md` بخش ۴.۲.۱ ثبت شد.

**۳) `ASSISTANT` به کلاس‌ها و درس‌ها دسترسی ندارد.** `api.md` بخش ۴.۲ ستون دسترسی را
`TEACHER` گذاشته و دستیار در آن نیست. افزودنش تغییر سیاست است، نه اصلاح.

**۴) `classroom` هیچ Repository ماژول دیگری را تزریق نمی‌کند.** `classroom.internal`
مستقل است؛ `tenant` و `common` و `audit` ماژول‌های OPEN‌اند و `security.audit` یک
`@NamedInterface` است. `ApplicationModulesTest.verify()` این را تضمین می‌کند.

**۵) تصمیم باز: هر کارمند مستأجر می‌تواند درسِ ساخته‌شده توسط همکارش را ویرایش یا
حذف کند.** «ریاضی پایه پنجم» در یک مدرسه یک رکورد مشترک است که چند معلم از آن
استفاده می‌کنند؛ محدودکردنش به سازنده، آن را غیرقابل استفاده می‌کرد. `created_by`
و `updated_by` ردش را نگه می‌دارند. اگر سیاست سخت‌گیرانه‌تری خواستید، نقطه تغییر
`SubjectController.STAFF_ONLY` و `SubjectService.require` است.

---

## ۱۴. ثبت فاز ۳ — دانش‌آموزان و ثبت‌نام (گام ۲ از ۳)

### ۱۴.۰ نتیجه

```text
mvn -B verify -Dtest.postgres.image=postgres:16-alpine

Tests run: 390, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

تست‌های تازه گام ۲:

| کلاس | تعداد | نوع |
|---|---|---|
| `NationalCodeTest` | ۱۲ | unit — رقم کنترلی کد ملی |
| `EnrollmentServiceAccessTest` | ۷ | unit — دسترسی، ظرفیت، تکراری |
| `StudentEnrollmentIsolationTest` | ۱۲ | `@SpringBootTest` + Testcontainers |

`ApplicationModulesTest.verify()` با وابستگی `classroom → student`
(از طریق `StudentDirectory`) سبز ماند.

### ۱۴.۱ محدوده ساخته‌شده

| مورد | جزئیات |
|---|---|
| مهاجرت `V10` | `students` · `class_students` · `uk_classrooms_tenant_id` |
| CRUD دانش‌آموز | `/api/v1/students` — Entity تا Controller + اعتبارسنجی کد ملی |
| ثبت‌نام | `GET|POST /classes/{id}/students` · `DELETE .../{studentId}` |
| داشبورد | `GET /classes/{id}/dashboard` — شمار دانش‌آموزان فعال و صندلی خالی |
| Facade | `StudentDirectory` — تنها راه خواندن دانش‌آموز از ماژول `classroom` |
| تست‌ها | `NationalCodeTest` · `EnrollmentServiceAccessTest` · `StudentEnrollmentIsolationTest` |

### ۱۴.۲ تصمیم‌های ثبت‌شده

**۱) دانش‌آموز دارایی مدرسه است، نه معلم.** هر کارمند مستأجر همه دانش‌آموزان
را می‌بیند. قید `teacherA_cannotReadTeacherB_students` روی مسیر ثبت‌نام
**کلاس** اعمال می‌شود (`requireAccessible`)، نه روی پرونده دانش‌آموز.

**۲) داشبورد عمداً تکالیف و نمرات ندارد.** برگرداندن صفر یعنی «چیزی نیست» —
و آن دروغ است تا فاز ۷ و ۸. Endpoint توخالی بدتر از Endpoint باز است.

**۳) دانش‌آموز حذف‌نرم‌شده از فهرست کلاس غایب می‌شود و صندلی آزاد می‌کند.**
ردیف `class_students` می‌ماند (ماژول `student` حق حذفش را ندارد)، ولی
`StudentDirectory` فقط فعال‌ها را برمی‌گرداند.

**۴) صفحه‌بندی فهرست دانش‌آموزان کلاس در حافظه است.** فیلتر دانش‌آموز
حذف‌شده نیاز به join بین‌ماژولی دارد که ممنوع است؛ اندازه کلاس‌های واقعی
این را قابل قبول می‌کند.

### ۱۴.۳ کارهای باقی‌مانده

| مورد | کجا |
|---|---|
| صفحه‌های Mini App برای کلاس‌ها و دانش‌آموزان | گام ۳ همین فاز |
| `GET /students/{id}/performance` و `/submissions` | فاز ۷ و ۸ |
| `STUDENT(خود)` روی `GET /students/{id}` | ورود دانش‌آموز — فاز بعد |
| بازتکرار تست‌های دیتابیسی روی PostgreSQL ۱۷ | محدودیت محیطی — بخش ۱۳.۳ |

---

## ۱۵. ثبت فاز ۴ — مدیریت وضعیت کاربر + اسکلت پنل ادمین (گام ۲)

### ۱۵.۰ نتیجه

```text
# بک‌اند
mvn -B verify -Dtest.postgres.image=postgres:16-alpine
Tests run: 402, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

# پنل ادمین
cd admin-panel && npm test && npm run build
Test Files  3 passed (3)
Tests       5 passed (5)
vite build  OK
```

تست‌های تازه گام ۲:

| کلاس / بسته | تعداد | نوع |
|---|---|---|
| `AdminUserManagementTest` | ۶ | `@SpringBootTest` + MockMvc + Testcontainers |
| `admin-panel` vitest | ۵ | unit — api-client، auth.store، مسیریابی |

### ۱۵.۱ محدوده ساخته‌شده

| مورد | جزئیات |
|---|---|
| `POST /admin/users/{id}/disable` | وضعیت `DISABLED` + ابطال همه Refresh Tokenها + `SECURITY_SENSITIVE_ACTION` |
| `POST /admin/users/{id}/enable` | بازگشت به `ACTIVE` از DISABLED/SUSPENDED |
| `POST /admin/users/{id}/suspend` | بدنه `{ "reason" }` اجباری؛ وضعیت `SUSPENDED` |
| اسکلت `admin-panel/` | Vite + React 19 + TS + Tailwind — ورود، داشبورد، فهرست کاربران |
| مستندات | وضعیت Endpointها در `api.md` §۴.۱ و §۴.۱۰ به‌روز شد |

### ۱۵.۲ تصمیم‌های ثبت‌شده

**۱) ادمین نمی‌تواند حساب خودش را disable/enable/suspend کند.** یک کلیک اشتباه
نباید سوپرادمین را بدون راه بازگشت قفل کند.

**۲) CSRF برای درخواست‌های Bearer روی زنجیره ادمین نادیده گرفته می‌شود.** توکن در
هدر است و مرورگر آن را خودکار نمی‌فرستد — همان استدلال Mini App. وقتی نشست ادمین
روی کوکی HttpOnly بیاید، CSRF دوباره برای آن مسیر اجباری می‌شود.

**۳) پنل ادمین فعلاً توکن را در `sessionStorage` نگه می‌دارد** (اسکلت فاز ۴). مسیر
نهایی کوکی HttpOnly + CSRF طبق `docs/admin-panel.md` هنوز پیاده نشده است.

### ۱۵.۳ کارهای باقی‌مانده فاز ۴

| مورد | توضیح |
|---|---|
| RBAC کامل از جداول `roles` / `permissions` | مجوزهای ریزدانه روی Endpointها — هنوز نقش‌محور |
| CRUD کاربر + تغییر نقش + بازنشانی رمز | ✅ بخش ۱۶ |
| مدیریت مستأجر و مدرسه | ✅ مستأجر؛ مدرسه جدا هنوز باز |
| Feature Flags و overrides | ✅ V14 + `/admin/features` |
| ArchUnit برای `@PreAuthorize` روی Endpointهای تغییردهنده | ✅ `ArchUnit_adminMutatingEndpointsRequirePreAuthorizeTest` |
| کوکی HttpOnly + CSRF کامل در پنل | |
| Audit UI و بقیه صفحات پنل | |

---

## ۱۶. ثبت فاز ۴ گام ۳ — CRUD کاربر، مستأجر، Feature Flags

### ۱۶.۰ نتیجه

```text
mvn -B verify -Dtest.postgres.image=postgres:16-alpine
Tests run: 429, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### ۱۶.۱ محدوده ساخته‌شده

| مورد | جزئیات |
|---|---|
| `POST/GET/PUT /admin/users` | ساخت ستاد با رمز، جزئیات، ویرایش پروفایل |
| `POST .../reset-password` | هش جدید + ابطال نشست‌ها؛ demote/reset خود ممنوع |
| `POST .../change-role` | جایگزینی نقش پلتفرمی SUPER_ADMIN/ADMIN |
| `GET\|POST\|PUT /admin/tenants` | داده واقعی از جدول `tenants` |
| `GET/PUT /admin/features` + `POST .../overrides` | کاتالوگ V14 + override مستأجر/کاربر |
| ArchUnit | متدهای تغییردهنده `/admin/**` باید `@PreAuthorize` داشته باشند |

### ۱۶.۲ تصمیم‌های ثبت‌شده

**۱) شماره Migration امکانات = V14.** V12 برای files/books و V13 برای AI
توسط کارهای موازی گرفته شده بود؛ features در V5 نبود.

**۲) نقش قابل تخصیص فقط SUPER_ADMIN و ADMIN است.** نقش‌های TENANT-level
از مسیر `change-role` پنل ادمین نمی‌آیند.

**۳) فهرست کاربران نقش‌ها را خالی برمی‌گرداند** تا N+1 نشود؛ جزئیات و
عملیات نوشتن `platformRoles` را پر می‌کنند.

### ۱۶.۳ کارهای باقی‌مانده فاز ۴

| مورد | توضیح |
|---|---|
| `DELETE /admin/users/{id}` | حذف نرم |
| مدیریت مدارس جدا (`/admin/schools`) | |
| RBAC ریزدانه از `permissions` روی Endpointها | فعلاً نقش‌محور |
| کوکی HttpOnly + CSRF کامل در پنل | |
| Audit UI و بقیه صفحات پنل | |

---

## ۱۷. ثبت فاز ۵ — Mock AI vertical slice (گام ۱)

### ۱۷.۰ نتیجه تست

```text
mvn -B clean test \
  -Dtest=AiMockVerticalSliceTest,ApplicationModulesTest,ArchitectureTest \
  -Dtest.postgres.image=postgres:16-alpine
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| کلاس | تعداد | نتیجه |
|---|---|---|
| `AiMockVerticalSliceTest` | ۴ | ✅ chat معلم، ۴۰۳ دانش‌آموز، مصرف، عدم نشت کلید |
| `ApplicationModulesTest` | ۲ | ✅ مرز ماژول `ai` |
| `ArchitectureTest` | ۹ | ✅ شامل بسته `ai` |

### ۱۷.۱ محدوده

| مورد | جزئیات |
|---|---|
| ماژول `ai` | `AiProvider` + `MockAiProvider` (`app.mock.ai=true`) + `AiCostCalculator` stub |
| Migration | `V13__ai_providers_models_usage.sql` — `ai_providers` / `ai_models` / `ai_usage_records` (V12 = files/books؛ V14 = features) |
| معلم | `POST /api/v1/ai/chat` — TEACHER؛ دانش‌آموز ۴۰۳ |
| ادمین | `GET|POST /admin/ai/providers`، `GET /admin/ai/models` — فقط `tokenHint` |
| تست | `AiMockVerticalSliceTest` — نشت کلید، chat، نقش، مصرف |

### ۱۷.۲ قید تأییدنشده (بدون تغییر)

Adapterهای Anthropic/OpenAI/… و نام فیلدهای JSONشان **هنوز حدس زده نشده‌اند**
(بخش ۴ همین سند). گام ۱ فقط Mock را پوشش می‌دهد.

### ۱۷.۳ کارهای باقی‌مانده فاز ۵

| # | مورد |
|---|---|
| ۲–۴ | `OpenAiCompatibleProvider` و Adapterهای واقعی پس از تأیید مستندات رسمی |
| ۶–۷ | Model Catalog sync + `ProviderResolver` / `ProviderBroker` / Circuit Breaker |
| ۸–۱۰ | هزینه کامل + سیستم Prompt + Resolver شش‌سطحی |
| ۱۱–۱۴ | زمینه خودکار دستیار، پنل Prompt، resolve-trace، گزارش مصرف |

---

## ۱۸. ثبت فاز ۶ — فایل + کتاب CRUD (گام ۱)

### ۱۸.۰ نتیجه

```text
mvn -B clean verify -Dtest.postgres.image=postgres:16-alpine
Tests run: 429, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

⚠️ تصویر تست: `postgres:16-alpine` (نه ۱۷ — محدودیت محیطی Docker Hub).
### ۱۸.۱ محدوده ساخته‌شده

| مورد | جزئیات |
|---|---|
| Migration `V12` | `files` · `books` · `book_chapters` · `book_pages` + FK ترکیبی مستأجر |
| ماژول `file` | `FileStorage` + `LocalFileStorage` + `FileCatalog` · ریشه `app.storage.local.base-path` |
| ماژول `book` | CRUD `/api/v1/books` · `POST/GET .../pages` · soft delete · `OcrService`/`MockOcrService` |
| Mini App | `BooksPage` فهرست/ساخت · `BookDetailPage` جزئیات+صفحات |
| تست | `BookTenantIsolationTest` · `LocalFileStoragePathTraversalTest` |

### ۱۸.۲ تصمیم‌های ثبت‌شده

**۱) شماره Migration = V12.** موازی با V13 (AI) و V14 (features)؛ نقشه راه
قدیمی V10 را books می‌دانست — معتبر نیست.

**۲) کتاب دارایی مستأجر است** (مثل دانش‌آموز)، نه فیلتر معلم سرکلاس.
جداسازی بین‌مستأجری با `@TenantId` + `TenantGuard` کافی است.

**۳) OCR فقط Interface + Mock.** هیچ شکل API ابری جعل نشده؛ صفحه با
آپلود متن جایگزین می‌گیرد و `ocr_status=SKIPPED`.

**۴) `files.tenant_id` در گام ۱ NOT NULL است.** ERD برای فایل پلتفرم
nullable می‌گوید؛ آن حالت بعداً با مدل جدا می‌آید.

### ۱۸.۳ کارهای باقی‌مانده فاز ۶

| # | مورد |
|---|---|
| ۳–۴ | PDF کامل + استخراج صفحه |
| ۵ | OCR واقعی (پس از تأیید مستندات رسمی ارائه‌دهنده) |
| ۶–۷ | تحلیل AI + `content_analyses` |
| ۸ | آپلود صفحه در UI · نمایش تصویر · اقدامات AI |
| ۹ | `storageBytes` در سقف پلن |
| — | API فصل‌ها (`book_chapters`) |

---

## ۱۹. ثبت فاز ۷ — تکلیف + ارسال Mini App (گام ۱)

### ۱۹.۰ نتیجه

```text
mvn -B test "-Dtest=AssignmentLifecycleTest,ApplicationModulesTest" "-Dtest.postgres.image=postgres:16-alpine"
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
  · AssignmentLifecycleTest: 5
  · ApplicationModulesTest: 2
```

⚠️ تصویر تست: `postgres:16-alpine`.
⚠️ `mvn clean verify` کامل در همین نشست به‌خاطر تداخل موازی ماژول‌های
دیگر (billing/exam/V17 روی classpath) ناپایدار بود؛ محدودهٔ فاز ۷ گام ۱
با تست‌های بالا و Modulith سبز شد.
### ۱۹.۱ محدوده ساخته‌شده

| مورد | جزئیات |
|---|---|
| Migration `V15` | `assignments` · `assignment_submissions` · stub `ai_evaluations` |
| Facade `ClassRoomAccess` | دسترسی کلاس برای ماژول‌های دیگر بدون شکستن Modulith |
| ماژول `assignment` | CRUD · publish (+ Mock Eitaa) · submissions متنی · soft delete |
| Mini App | `AssignmentsPage` · `AssignmentDetailPage` (انتشار + فرم پاسخ) |
| تست | `AssignmentLifecycleTest` — tenant isolation · `student_cannotSeeOtherStudentsSubmission` · publish mock |

### ۱۹.۲ تصمیم‌های ثبت‌شده

**۱) شماره Migration = V15.** موازی با نقشه راه قدیمی V11؛ بزرگ‌ترین موجود + ۱.

**۲) پاسخ فقط از Mini App.** هیچ webhook/receive از ایتا نیست (`docs/eitaa.md` بخش ۶).

**۳) مسیر API ارسال = `POST /assignments/{id}/submissions`.** نه `/submit`.

**۴) شکست ارسال ایتا، انتشار را باطل نمی‌کند.** همان قرارداد `EitaaAdapter` / `SendFailureDoesNotRollbackAssignmentTest`.

**۵) حریم ارسال دانش‌آموز** با `submitted_by_user_id` تا وقتی لینک user↔student کامل شود.

### ۱۹.۳ کارهای باقی‌مانده فاز ۷

| # | مورد |
|---|---|
| ۲ | Structured کامل `ai_evaluations` |
| ۳ | `teacher_reviews` |
| ۶–۸ | تصحیح AI + بازبینی + reevaluate-all |
| ۹ | اعلان مسیر ب |
| ۱۰ | UI تصحیح/بازبینی · آپلود تصویر پاسخ |
| ۱۱ | تست سقف همزمان |

---

---

## 20. ثبت فاز ۱۱ + ۱۲ — گام ۱ (گزارش + Production basics)

### 20.0 محدوده

**Migration:** `V19__report_aggregation_indexes.sql` (ایندکس created_at/status برای تجمیع)

**فاز ۱۱ گام ۱**
- `GET /api/v1/admin/dashboard/charts?period=30d` — daily new users / tenants / subscriptions
- `GET /api/v1/admin/reports/{users,subscriptions,ai,revenue}` — تجمیع واقعی
- `GET /api/v1/admin/reports/users.csv`
- صفحه Reports در admin-panel
- تست: `AdminReportsAggregationTest`

**فاز ۱۲ گام ۱**
- `.github/workflows/ci.yml` — backend JDK 25 + Testcontainers، miniapp، admin-panel
- gitleaks job اختیاری (`continue-on-error` / باینری غایب)
- `docker-compose` پروفایل `optional` برای redis/minio (stub)
- `RateLimiter` + `InMemoryRateLimiter` + فیلتر روی `/auth/login`
- `docs/runbook.md`
- چک‌لیست `docs/security.md` §۱۶ فقط موارد واقعاً درست تیک خورد
- Actuator `info`: env/java/os/git خاموش

### 20.1 وضعیت صادقانه

| فاز | وضعیت گام ۱ |
|---|---|
| ۱۱ | **PARTIAL** — API + CSV + صفحه ساده؛ نمودار UI کامل / Excel / کش / churn باز |
| ۱۲ | **PARTIAL** — CI + rate-limit حافظه‌ای + runbook؛ Redis واقعی / 2FA / Prometheus / backup خودکار باز |

### 20.2 فرمان تأیید

```text
mvn -B verify "-Dtest.postgres.image=postgres:16-alpine"
Tests run: 451, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

⚠️ تصویر تست: `postgres:16-alpine` (نه ۱۷).

---

## ۲۱. ثبت فاز ۴/۵ — تکمیل شکاف‌ها (V20/V21)

### ۲۱.۰ نتیجه

```text
mvn -B verify "-Dtest.postgres.image=postgres:16-alpine"
Tests run: 481, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

⚠️ تصویر تست: `postgres:16-alpine` · JDK 25 (toolchain).

### ۲۱.۱ محدوده ساخته‌شده

| مورد | جزئیات |
|---|---|
| Migration `V20` | `ai_provider_assignments` — شش سطح |
| Migration `V21` | `prompts` / `prompt_versions` / `prompt_assignments` + seed `AI_CHAT_SYSTEM` |
| فاز ۴ | soft-delete کاربر · مدارس روی `tenants` نوع SCHOOL · `GET /admin/csrf` · UI مستأجرها/امکانات |
| فاز ۵ | `PromptRenderer`/`PromptResolver` · `ProviderResolver`/`ProviderBroker` · Publish/Rollback/Playground · `POST …/providers/{id}/test` · Mini App AssistantPage |
| تست | `AiPhase5CoreTest` — teacher 403، student 403، resolver، failover، publish/rollback، عدم نشت کلید |

### ۲۱.۲ باقی‌ماندهٔ صادقانه

| مورد | جزئیات |
|---|---|
| کوکی HttpOnly ورود ادمین | عمداً نرفت تا Bearer تست‌ها نشکنند؛ CSRF Endpoint هست |
| Adapter واقعی Anthropic/OpenAI/… | بدون حدس JSON؛ فقط اسکلت OpenAI-compatible با TODO |
| Circuit Breaker / Compare نسخه‌ها / resolve-trace | باز |
| UI پنل Prompt Editor | API هست؛ UI بعداً |
| زمینه خودکار کلاس در chat | هنوز پیام خام + پرامپت سیستمی |

---

## ۲۲. ثبت فاز ۸/۹ گام ۲ — V24/V25

### ۲۲.۰ محدوده

**Migration:** `V24__exam_blueprints.sql` · `V25__eitaa_channels_automation.sql`

**فاز ۸ گام ۲**
- `POST /exams/generate` — Mock AI · MCQ + یک ESSAY · ذخیره blueprint
- تصحیح AI ساختاریافته برای SHORT/ESSAY روی submit
- `POST /exams/{id}/attempts/{aid}/grade` — بازنویسی معلم
- `GET /exams/{id}/results` · `GET /exams/attempts/{aid}/report`
- رد submit پس از `started_at + durationMinutes`
- Mini App: `ExamTakePage` · نتایج · لینک تنظیمات

**فاز ۹ گام ۲**
- کانال CRUD + send · `automation_rules` · `message_credits`
- `@Scheduled` جاب PENDING→SENT/FAILED با پارامتر `date`
- موتور قواعد روی رویدادهای داخلی (`AssignmentPublishedEvent` و …)
- `pin` / `viewCountForDelete` در API ارسال
- Mini App: `GroupsPage`
- تست‌ها: `ExamPhase8FeaturesTest` · `EitaaPhase9AutomationTest` · ArchUnit webhook موجود

### ۲۲.۱ فرمان تأیید (تست‌های فاز ۸/۹ گام ۲)

```text
mvn -B test "-Dtest=ExamPhase8FeaturesTest,EitaaPhase9AutomationTest,EitaaOutboundVerticalSliceTest,ArchUnit_noWebhookControllerTest,ExamTenantIsolationTest" "-Dtest.postgres.image=postgres:16-alpine"
Tests run: 6+ (ExamPhase8FeaturesTest 4 · EitaaPhase9AutomationTest 2) — Failures: 0
BUILD SUCCESS
```

⚠️ تصویر تست: `postgres:16-alpine`. جاب زمان‌بندی در پروفایل تست با
`app.eitaa.scheduler.enabled=false` خاموش است مگر تست صریحاً روشن کند.

---

## ۲۳. ثبت فاز ۶/۷ گام ۲ — V22/V23

### ۲۳.۰ نتیجه (محدودهٔ این مالکیت)

```text
mvn -B test "-Dtest=BookLibraryPhase6Test,AssignmentLifecycleTest,LocalFileStoragePathTraversalTest" "-Dtest.postgres.image=postgres:16-alpine"
  · BookLibraryPhase6Test: 5 (PDF · analyze · chapters · OCR · path-traversal)
  · AssignmentLifecycleTest: 7 (incl. student_cannotSeeOtherStudentsSubmission · evaluate · review append-only)
  · LocalFileStoragePathTraversalTest: 4
BUILD SUCCESS
```

⚠️ تصویر تست: `postgres:16-alpine`.
⚠️ `mvn clean verify` کامل در محیط موازی با clean روی `target` ناپایدار بود؛ محدودهٔ V22/V23 با تست‌های بالا سبز شد.

### ۲۳.۱ محدوده ساخته‌شده

| مورد | جزئیات |
|---|---|
| Migration `V22` | `content_analyses` · سقف اختیاری `storageBytes` در `plan_limits` |
| Migration `V23` | گسترش `ai_evaluations` · `teacher_reviews` Append-Only |
| PDF | PDFBox 3 · `POST /books/{id}/pdf` · multipart create/pages |
| فصل | `GET\|POST /books/{id}/chapters` |
| OCR | `POST /books/{id}/pages/{n}/ocr` · MockOcr |
| تحلیل AI | `ContentAnalysisAi` facade · analyze/questions/teaching-plan · ذخیره در `content_analyses` |
| storageBytes | `FileCatalog.totalSizeBytes()` · `EntitlementService.requireStorageCapacity` |
| تصحیح/بازبینی | `POST .../submissions/{sid}/evaluate\|review` · `reevaluate-all` · اعلان Mock ایتا |
| Mini App | آپلود صفحه/PDF + تحلیل · UI تصحیح/بازبینی تکلیف |

### ۲۳.۲ باقی‌ماندهٔ صادقانه

| مورد | جزئیات |
|---|---|
| OCR ابری واقعی | عمداً باز — فقط Interface + Mock |
| Adapter واقعی Provider | فاز ۵ |
| Deep Link asg_ E2E واقعی | نیاز به توکن ایتا |
| سقف همزمان usageLimit | فاز ۷ مورد ۱۱ |

---

## ۲۴. ثبت برنامهٔ اصلاح ۷ موردی — گام A (A1a · A2 · امنیت و کانفیگ)

### ۲۴.۰ نتیجه

```text
cd backend && mvn -B clean verify        # روی درخت قفل‌شده
  Tests run: 508, Failures: 0, Errors: 0, Skipped: 0
  (۴۸۷ پایه + ۱۸ آزمون گام A2 + ۳ آزمون نگاشت توکن)
BUILD SUCCESS · Total time: 05:44 min

cd miniapp
  npm run typecheck  → EXIT 0
  npm run test       → 13 files · 156 tests · همه سبز
  npx eslint src     → EXIT 0 (0 problem)
  npm run build      → EXIT 0 · باندل پاک (۰ مقدار ممنوع) · built in 3.99s
  npm run size-check → 132.25 kB gzipped از سقف 150 kB ✅
```

⚠️ `npm run build` هشدار می‌دهد که یک chunk پس از minify بزرگ‌تر از ۴۰۰kB است
(`index-*.js` = ۴۶۵.۵۶ kB، gzip ۱۳۳.۸۲ kB). این هشدار **دروازه را رد نمی‌کند** —
بودجهٔ واقعی `size-check` است و با ۱۳۲.۲۵ kB سبز است — ولی اگر بعداً چانک از سقف
۱۵۰ kB گذشت، راه‌حل code-splitting با `import()` دینامیک است، نه بالا بردن سقف.

🔴 **دو اجرای پیشین `clean verify` عمداً کنار گذاشته شدند — و دلیلش مهم است.**
آن‌ها به‌خاطر شکست آزمون رد نشدند، بلکه چون فایل‌های `backend/src/main/java` **در
حین اجرای Maven** ویرایش شدند. افزونهٔ Java در VS Code هم‌زمان `target/classes` را
بازنویسی کرد و کلاس‌ها ناپدید شدند؛ نتیجه، شکست **همهٔ** `@SpringBootTest`ها با
`No qualifying bean of type '...Repository' available` بود — یعنی خطایی که به کد
اشاره می‌کرد ولی ریشه‌اش در ابزار بود. **درس ثبت‌شده: درخت را قفل کن، بعد build.**

⚠️ در همان لاگ‌های بی‌اعتبار، ردپای `ConstraintViolationException`
(`fk_audit_tenant`) و `java.net.ConnectException: Connection refused` داخل
کلاس‌هایی بودند که خودشان `Failures: 0, Errors: 0` گزارش می‌کردند — موارد
مدیریت‌شده، نه شکست.

### ۲۴.۱ محدودهٔ ساخته‌شده

| مورد | جزئیات |
|---|---|
| ویرایش/حذف گروه | `PUT\|DELETE /api/v1/eitaa/groups/{id}` · حذف **نرم** (`deleted_at`) |
| ویرایش/حذف کانال | `PUT\|DELETE /api/v1/eitaa/channels/{id}` |
| بررسی اتصال | `POST .../groups/{id}/verify` و `.../channels/{id}/verify` |
| سلامت ربات | `GET /api/v1/eitaa/health` — بر پایهٔ `getMe`؛ هیچ پیامی نمی‌فرستد |
| DTO تازه | `UpdateEitaaGroupRequest` · `UpdateEitaaChannelRequest` |
| قرارداد سه‌حالته | `null` = تغییر نده · `""` روی `title`/`username` = پاک کن · `""` روی `type` = تغییر نده (ستون `NOT NULL`) |
| اعتبارسنجی `chat_id` | **عمداً سهل‌گیر**: null · خالی · >۱۰۰ نویسه · فاصلهٔ داخلی · `http(s)://` |
| اعتبارسنجی `type` | آینهٔ `V17: chk_eitaa_groups_type CHECK (type IN ('GROUP','CHANNEL'))` |
| سقف طول | `title` = ۲۵۰ · `username` = ۱۰۰ (از `length` ستون‌ها) |
| Mini App — ماندگاری نشست | `localStorage` با کلید `smart-teacher.session.v1` + مهاجرت یک‌باره از `sessionStorage` |
| Mini App — بازگشت از پس‌زمینه | `refreshOnResume()` روی `visibilitychange`/`pageshow`/`focus` |
| Mini App — بن‌بست ورود | وضعیت `blocked` جدا از `unauthenticated`، با اکشن «بازکردن دوباره در ایتا» |
| نگاشت نام توکن | `app.eitaa.bot-token` و `app-token` حالا در **هر دو** `application-dev.yml` و `application-prod.yml` |
| آزمون‌ها | `EitaaOutboundCrudTest` (۱۳) · `ChatIdFormatTest` (۵) · `EitaaTokenBindingContractTest` (۳) |

### ۲۴.۲ نقص‌های واقعی که در بازبینی پیدا و رفع شدند

| # | نقص | چرا خطرناک بود | رفع |
|---|---|---|---|
| ۱ | بررسی تکراری‌بودن `chat_id` دامنهٔ **مستأجر** داشت، ولی `uk_eitaa_groups_chat_id` / `uk_eitaa_channels_chat_id` **سراسری**اند | بررسی برنامه برای رکورد مستأجر دیگر `false` می‌داد و بعد `commit` با نقض یکتایی می‌افتاد → کاربر به‌جای «قبلاً ثبت شده» پیام گمراه‌کنندهٔ ۴۰۹ «تناقض وضعیت» می‌گرفت | `@Query(nativeQuery = true)` → `existsByEitaaChatIdAnyTenant` / `...AnyTenantAndIdNot`. تأیید شد **نه** RLS و **نه** `@Filter` در پروژه نیست |
| ۲ | مقدار `type` هرگز با `chk_eitaa_groups_type` سنجیده نمی‌شد | `"FOO"` به‌جای ۴۰۰، یک ۴۰۹ گمراه‌کننده می‌داد | `GROUP_TYPES` + `requireValidGroupType` |
| ۳ | `POST` از اعتبارسنجی تازه رد می‌شد | `"my group"` یا یک URL در POST پذیرفته و در PUT رد می‌شد — ناسازگاری | همان helperها به `createGroup` / `createChannel` وصل شد |
| ۴ | `@Valid` روی PUTهای تازه **بی‌اثر** بود | DTOها هیچ قید Bean Validation ندارند؛ `@Valid` فقط این توهم را می‌ساخت که بدنه در مرز سنجیده می‌شود | `@Valid` برداشته شد + کامنت توضیحی؛ سرویس همان ۴۰۰ `VALIDATION_FAILED` را می‌دهد |
| ۵ | نگاشت `app.eitaa.*` در `application-prod.yml` **نبود** | `EITAA_BOT_TOKEN` — نامی که سه سند آموزش می‌دادند — در Production بی‌صدا نادیده گرفته می‌شد. `EitaaYarBotAdapter` فقط `log.warn` می‌دهد، پس استقرار **سالم بالا می‌آمد** (`/actuator/health` → UP) و تازه موقع اولین ارسال خطا می‌داد | نگاشت دو‌نامی در هر دو پروفایل با پیش‌فرض **خالی** (اصل «هیچ مقدار پیش‌فرضی» نقض نشد) |
| ۶ | ادعای نادرست در `EitaaProperties` | می‌گفت «نبود توکن باعث می‌شود برنامه بالا نیاید». برای توکن ربات **غلط** است: نه راه‌اندازی متوقف می‌شود و نه health قرمز می‌شود | ادعا با شرح رفتار واقعی جایگزین شد |
| ۷ | افشای **جزئی** راز در `docs/eitaa-setup.md` | شناسهٔ ربات واقعی به‌عنوان «نمونه» در دو بلوک کد نوشته شده بود. شناسهٔ ربات نیمهٔ اول توکن است | مقدار ساختگی جایگزین شد؛ حتی در توضیحِ حذف هم تکرار نشد |

⚠️ سه ادعای نادرست دیگر در مستندات که بررسی و **رد** شدند (ثبت می‌شوند تا دوباره
وقت نگیرند): «`backend/.dockerignore` وجود ندارد» (هر دو فایل موجودند) ·
«`APP_MOCK_EITAA` متغیر مرده است» (با relaxed binding به `app.mock.eitaa` می‌بندد، و
`docs/eitaa-setup.md` بخش ۵.۳ درست توضیح داده) · «`application.yml` فایل `.env` را
نمی‌خواند» (می‌خواند: `spring.config.import: optional:file:./.env[.properties]`).

✅ **بررسی جهشی روی آزمون تازه:** `EitaaTokenBindingContractTest` به‌طور موقت با
برداشتن نگاشت از `application-prod.yml` اجرا شد → **هر ۳ آزمون شکستند**
(`Failures: 3`)؛ سپس فایل بازیابی شد. پس آزمون توخالی نیست و همان نقص را قفل می‌کند.

### ۲۴.۳ باقی‌ماندهٔ صادقانه

| مورد | وضعیت |
|---|---|
| **آزمون واقعی ایتا** | ❌ انجام **نشده**. `verify` فقط اعتبار توکن و قالب `chat_id` را می‌سنجد؛ ایتا هیچ متدی برای بررسی وجود یا دسترسی گفت‌وگو ندارد. تنها تأیید قطعی، ارسال یک پیام آزمایشی واقعی است — و پیش از هر ارسال باید `chat_id`، گروه/کانال بودن، و متن دقیق **دوباره** تأیید شود (ارسال بازگشت‌ناپذیر است: `deleteMessage`/`editMessageText` وجود ندارد) |
| گام A1b (ورود تکرارپذیر) | با توجه به حل کامل مشکل با پایداری توکن در localStorage (A1a) و عدم نیاز به initData در بازگشت از پس‌زمینه، کنترل امنیتی بازپخش یک‌بارمصرف حفظ شد؛ RedisReplayGuard در A3 پیاده‌سازی شد |
| گام A3 (سخت‌سازی پروداکشن) | ✅ **تکمیل شد**: محدودیت نرخ روی تمامی مسیرهای احراز هویت (`/auth/login`, `/auth/eitaa`, `/auth/refresh`, `/auth/logout`, `/auth/refresh-admin`, `/auth/logout-admin`) پیاده و آزموده شد · `RedisReplayGuard` بر پایه `app.redis.enabled` افزوده شد · `.env.production.example` ایجاد شد · آزمون‌ها به ۵۱۱ رسید |
| صفحهٔ ایتا در پنل ادمین | ✅ پیاده‌سازی کامل صفحه `EitaaPage.tsx` به همراه نمایش سلامت ربات و راهنمای اتصال |
| گام B (پنل ادمین و CRUD کامل) | ✅ **تکمیل شد**: سیستم طراحی (`components/ui/*`)، `AdminLayout` با سایدبار تیره و نشانگرهای مدرن، داشبورد تحلیلی با نمودارهای SVG و کارت‌های آماری، مسیرهای جدید بک‌اند (`DELETE /schools`, `DELETE /tenants`, `DELETE/disable /plans`, `enable/DELETE /coupons`, `cancel/extend /subscriptions`, `PUT/DELETE /ai/providers`, `POST/PUT/DELETE /ai/models`, `DELETE /prompts`, `POST /features`, `GET/PUT /users/{id}/roles`, `GET /audit-log`, `GET/PUT /settings`) با رعایت کامل `@PreAuthorize` و ArchUnit (8 از 8 تست سبز) |
| گام D (استقرار پروداکشن و Nginx) | ✅ **تکمیل شد**: `docker-compose.prod.yml` با بستن پورت‌های مستقیم دیتابیس و ردیس، حل تله iframe ایتا در `nginx/prod.conf` با مجاز کردن `frame-ancestors https://eitaa.com` و محافظت پنل ادمین با `DENY`، نگارش `docs/deployment.md` و `docs/production-checklist.md` و اسکریپت `scripts/run-prod.ps1` |
| `mvn -B verify` وابستگی به Docker | Testcontainers روی `postgres:17-alpine`؛ بدون Docker Desktop سبز نمی‌شود |
| مقادیر تأییدنشدهٔ ایتا | `maxMessageLength` · `sendRatePerSecond` · `sendFileMethod` — همچنان محافظه‌کارانه و علامت‌خورده در `EitaaProperties` (چک‌لیست `docs/eitaa.md` بخش ۱۱.۲) |

---

