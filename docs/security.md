# معماری امنیت

> بخش ۵۳، ۵۶، ۶۰ خواسته‌ها · نسخه 1.0

---

## ۱. مدل تهدید (Threat Model)

| # | تهدید | سطح خطر | کنترل |
|---|---|---|---|
| T1 | معلم A داده معلم B را می‌بیند | **بحرانی** | `@TenantId` + `TenantGuard` + تست چندمستأجری |
| T2 | نشت API Key Providerهای AI | **بحرانی** | AES-256-GCM + Write-Only DTO + ممنوعیت Log |
| T3 | دانش‌آموز به API معلم دسترسی پیدا می‌کند | بالا | RBAC + بررسی نقش در سطح متد |
| T4 | دسترسی دانش‌آموز به تکلیف دانش‌آموز دیگر | بالا | بررسی مالکیت رکورد در Service |
| T5 | IDOR با تغییر UUID در URL | بالا | `TenantGuard` + `404` به‌جای `403` |
| T6 | کاربر اشتراک منقضی از Feature پرمیوم استفاده می‌کند | بالا | Entitlement در هر درخواست |
| T7 | کاربر سقف مصرف را دور می‌زند (Race Condition) | بالا | قفل ردیف + رزرو اتمیک |
| T8 | جعل `initData` ایتا | **بحرانی** | اعتبارسنجی HMAC-SHA256 + بررسی `auth_date` |
| T9 | Replay حمله با JWT دزدیده‌شده | متوسط | عمر کوتاه Access + چرخش Refresh + تشخیص استفاده مجدد |
| T10 | تزریق Prompt (Prompt Injection) از ورودی دانش‌آموز | متوسط | جداسازی System/User + Sanitization + عدم اجرای دستور |
| T11 | ارتقای سطح دسترسی (Privilege Escalation) | **بحرانی** | اعتبارسنجی نقش در سرور، هرگز از Body/Header سرور |
| T12 | Admin اشتباهاً پلن را تخریب می‌کند | متوسط | Audit + Soft Delete + تأیید دومرحله‌ای برای عملیات مالی |

---

## ۲. معماری احراز هویت (دو مسیر مستقل)

```mermaid
graph TB
    subgraph PathA["مسیر A — معلم و دانش‌آموز (Mini App ایتا)"]
        A1["Mini App باز می‌شود"] --> A2["ایتا initData می‌دهد"]
        A2 --> A3["POST /api/v1/auth/eitaa"]
        A3 --> A4["اعتبارسنجی HMAC-SHA256"]
        A4 --> A5["ایجاد/یافتن User + Tenant"]
        A5 --> A6["صدور JWT"]
    end

    subgraph PathB["مسیر B — SUPER_ADMIN / ADMIN / SCHOOL_ADMIN (وب)"]
        B1["فرم ورود"] --> B2["POST /api/v1/auth/login"]
        B2 --> B3["BCrypt / Argon2 verify"]
        B3 --> B4["بررسی قفل حساب"]
        B4 --> B5["اختیاری: TOTP 2FA"]
        B5 --> B6["صدور JWT"]
    end

    A6 --> J["JWT (Access + Refresh)"]
    B6 --> J
```

دو مسیر **کاملاً جدا** هستند: `SecurityFilterChain` جداگانه، مسیر جداگانه، و **هیچ‌کدام نمی‌توانند توکن دیگری را جعل کنند** چون `token_use` در Claim و امضای مشترک بررسی می‌شود.

---

## ۳. طراحی JWT

### Access Token

```json
{
  "iss": "smart-teacher",
  "sub": "0191f2a3-...",
  "aud": "smart-teacher-api",
  "exp": 1759000000,
  "iat": 1758999100,
  "jti": "0191f2a3-...",
  "tenant_id": "0191f2b4-...",
  "roles": ["TEACHER"],
  "plan_id": "0191f2c5-...",
  "token_use": "access",
  "scope": "miniapp"
}
```

| پارامتر | مقدار | دلیل |
|---|---|---|
| عمر Access Token | **۱۵ دقیقه** | حداقل پنجره سوءاستفاده |
| الگوریتم | **HS256** با کلید ۲۵۶ بیتی از Env | تک‌سرویسی؛ RS256 در آیندهٔ چندسرویسی |
| محل ذخیره (Mini App) | Access: حافظه · Refresh: `localStorage` | Access کوتاه‌عمر است و persist نمی‌شود؛ Refresh باید از بسته‌شدن WebView جان سالم ببرد (پایین را ببینید) |
| محل ذخیره (Admin) | **HttpOnly + Secure + SameSite=Strict Cookie** | پنل ادمین ریسک بالاتری دارد |
| Clock Skew | ۳۰ ثانیه | |

### Refresh Token

- عمر ۳۰ روز، **در دیتابیس ذخیره می‌شود** (نه فقط امضا) تا قابل باطل‌سازی باشد.
- **چرخش (Rotation):** هر بار استفاده، توکن قبلی باطل و توکن جدید صادر می‌شود.
- **تشخیص استفاده مجدد:** اگر توکنی که قبلاً مصرف شده دوباره استفاده شود → **کل خانواده توکن باطل می‌شود** و رویداد امنیتی CRITICAL ثبت می‌گردد. این نشانه قطعی سرقت توکن است.

```java
@Service
public class RefreshTokenService {
    @Transactional
    public TokenPair rotate(String rawToken) {
        RefreshToken stored = repo.findByHash(hash(rawToken))
                .orElseThrow(InvalidTokenException::new);

        if (stored.isRevoked()) {
            // ⚠️ استفاده مجدد از توکن باطل = نشانه سرقت
            securityAudit.critical(SecurityEvent.TOKEN_REUSE_DETECTED, stored);
            repo.revokeFamily(stored.getFamilyId());
            throw new TokenReuseDetectedException();
        }
        stored.revoke();
        return issue(stored.getUser(), stored.getTenantId(), stored.getFamilyId());
    }
}
```

> توکن خام **هرگز** در دیتابیس ذخیره نمی‌شود — فقط `SHA-256` آن. اگر دیتابیس لو برود، توکن‌ها قابل استفاده نیستند.

#### محل ذخیره Refresh Token در Mini App — و تغییر تصمیم

**تصمیم پیشین:** `sessionStorage`. **تصمیم فعلی:** `localStorage` (کلید نام‌فضا
`smart-teacher.session.v1`).

⚠️ تصمیم پیشین یک **باگ پروداکشنی** می‌ساخت، نه فقط یک ضعف محتمل. زنجیره‌اش:

```text
پس‌زمینه‌شدن WebView ایتا → sessionStorage پاک می‌شود
  → تنها راه ورود، initData است
    → initData یک‌بارمصرف و ۵دقیقه‌معتبر است
      → سرور رد می‌کند و کاربر در حلقه «دوباره تلاش کنین» گیر می‌کند
```

پس ماندگاری نشست، **خودِ راه‌حل** است و ریسک XSSِ باقی‌مانده مهارشده:

| کنترل | اثر |
|---|---|
| چرخش یک‌بارمصرف | هر استفاده از توکن، توکن تازه می‌دهد؛ نسخه دزدیده‌شده فقط یک بار کار می‌کند |
| تشخیص استفاده مجدد خانوادگی | مصرف دوباره → **کل خانواده** باطل می‌شود؛ سرقت *دیده* می‌شود نه بی‌صدا |
| Access Token در حافظه | توکن ۱۵دقیقه‌ای هرگز persist نمی‌شود |
| پاک‌سازی در خروج و مرگ نشست | `clearRefreshToken()` هر دو محل و هر دو کلید را می‌شوید |
| نشستن در `localStorage` همان WebView | دسترسی از بیرون WebView ممکن نیست |

⚠️ **آنچه این تصمیم حل نمی‌کند:** اگر XSS داخل خودِ WebView اجرا شود،
`localStorage` خوانده می‌شود. تفاوت با `sessionStorage` در آن سناریو فقط
<i>مدت</i> سوءاستفاده است — که با چرخش و تشخیص استفاده مجدد کوتاه می‌شود.
جایگزین کامل، کوکی `HttpOnly` است که برای برنامک داخل iframe ایتا عملی نیست.

---

## ۴. اعتبارسنجی `initData` ایتا

این حساس‌ترین نقطه ورود سیستم است. کامل در `docs/eitaa.md` توضیح داده شده، ولی خلاصه امنیتی:

```text
1. دریافت initData خام از Mini App
2. جدا کردن فیلد hash
3. ساخت data_check_string از باقی فیلدها (مرتب‌شده الفبایی، joined با \n)
4. محاسبه HMAC-SHA256 با کلید مشتق‌شده از Bot Token
5. مقایسه با hash — با MessageDigest.isEqual (زمان-ثابت، ضد Timing Attack)
6. بررسی auth_date — حداکثر ۵ دقیقه قدیمی (ضد Replay)
7. بررسی user.id در نتیجه
```

**نکات امنیتی الزامی:**
- مقایسه **همیشه** با `MessageDigest.isEqual` — هرگز `String.equals` (نشت زمان).
- `auth_date` قدیمی‌تر از ۵ دقیقه رد می‌شود.
- `initData` **یک‌بارمصرف** در Redis/حافظه کش می‌شود تا Replay ممکن نباشد.
- الگوریتم دقیق مشتق‌سازی کلید **باید** از مستندات رسمی ایتا تأیید شود — تا آن زمان در `docs/eitaa.md` به‌عنوان «نیازمند تأیید» علامت‌گذاری شده است و Mock استفاده می‌شود.

---

## ۵. احراز هویت وب (Admin Panel) — سخت‌گیری بیشتر

پنل ادمین دسترسی به تنظیمات AI، کلیدها و داده مالی دارد. پس:

| کنترل | پیاده‌سازی |
|---|---|
| هش رمز | **Argon2id** (ترجیح) — پیکربندی: `m=19456 KiB, t=2, p=1`. اگر کتابخانه در دسترس نبود، BCrypt با cost 12 |
| قفل حساب | پس از ۵ تلاش ناموفق → قفل ۱۵ دقیقه‌ای (شمارنده در DB) |
| 2FA | TOTP (اختیاری در فاز ۴، اجباری برای Super Admin در فاز ۱۲) |
| محدودیت نرخ | ۱۰ درخواست در دقیقه به `/auth/login` بر اساس IP + Username |
| کوکی | `HttpOnly; Secure; SameSite=Strict; Path=/api` |
| CSRF | چون از کوکی استفاده می‌شود → توکن CSRF اجباری برای متدهای غیر-GET |
| Session Fixation | صدور JWT جدید پس از هر تغییر سطح دسترسی |

**تفکیک مسیرها:**

```java
@Bean @Order(1)
SecurityFilterChain adminChain(HttpSecurity http) {
    http.securityMatcher("/api/v1/admin/**", "/api/v1/auth/login", "/api/v1/auth/refresh-admin")
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/v1/auth/login").permitAll()
            .anyRequest().hasAnyRole("SUPER_ADMIN", "ADMIN"))
        .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS));
    return http.build();
}

@Bean @Order(2)
SecurityFilterChain miniAppChain(HttpSecurity http) {
    http.securityMatcher("/api/v1/**")
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/v1/auth/eitaa", "/api/v1/public/**",
                             "/actuator/health/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .csrf(csrf -> csrf.disable())          // JWT در Header، نه کوکی
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS));
    return http.build();
}
```

---

## ۶. RBAC و مجوزها

### نقش‌ها

| نقش | سطح | توضیح |
|---|---|---|
| `SUPER_ADMIN` | پلتفرم | دسترسی کامل به همه‌چیز |
| `ADMIN` | پلتفرم | دسترسی عملیاتی، بدون تغییرات بحرانی (قیمت، کلید) |
| `SCHOOL_ADMIN` | مستأجر مدرسه | مدیریت معلمان مدرسه، آمار تجمیعی |
| `TEACHER` | مستأجر | دامنه آموزشی خودش |
| `STUDENT` | مستأجر | فقط تکالیف و آزمون‌های خودش |

### مدل مجوز (Resource:Action)

```text
CLASS:CREATE  CLASS:READ  CLASS:UPDATE  CLASS:DELETE
STUDENT:CREATE  STUDENT:READ  STUDENT:UPDATE  STUDENT:DELETE
ASSIGNMENT:CREATE  ASSIGNMENT:GRADE  ASSIGNMENT:SUBMIT
EXAM:CREATE  EXAM:GRADE  EXAM:TAKE
BOOK:UPLOAD  BOOK:ANALYZE
AI:USE  AI:CONFIGURE
PROMPT:CREATE  PROMPT:PUBLISH  PROMPT:ASSIGN
PROVIDER:CREATE  PROVIDER:UPDATE  PROVIDER:VIEW_KEY
PLAN:CREATE  PLAN:UPDATE  SUBSCRIPTION:MANAGE
PAYMENT:VIEW  PAYMENT:REFUND
USER:MANAGE  TENANT:MANAGE  AUDIT:VIEW  SYSTEM:CONFIGURE
```

نقش → مجوز در جدول `role_permissions` نگهداری می‌شود، پس **نقش سفارشی در آینده بدون تغییر کد** قابل ساخت است.

```java
@PreAuthorize("hasAuthority('ASSIGNMENT:GRADE')")
public EvaluationResponse grade(UUID submissionId) { ... }
```

`@EnableMethodSecurity` فعال است. **همه** Endpointهای تغییردهنده حداقل یک `@PreAuthorize` دارند.

### قاعده «مالکیت» علاوه بر «مجوز»

مجوز می‌گوید «آیا این نقش اجازه این عمل را دارد؟» ولی کافی نیست. باید بپرسیم «آیا این کاربر مالک این رکورد است؟»

```java
private AssignmentSubmission loadOwned(UUID id) {
    AssignmentSubmission s = repo.findById(id).orElseThrow(ResourceNotFoundException::new);
    tenantGuard.requireOwned(s);                      // لایه ۱: مستأجر
    if (currentUser().hasRole(STUDENT)) {
        studentAccessGuard.requireOwnSubmission(s);   // لایه ۲: مالکیت
    }
    return s;
}
```

**هر دو لایه همیشه با هم.** یکی بدون دیگری کافی نیست.

---

## ۷. رمزنگاری Secretها

### الگوریتم: AES-256-GCM

```java
@Component
public class SecretEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;      // ۹۶ بیت — استاندارد GCM
    private static final int GCM_TAG_LENGTH = 128;    // بیت

    private final SecretKey masterKey;
    private final SecureRandom random = new SecureRandom();

    public SecretEncryptor(@Value("${security.encryption.master-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "ENCRYPTION_MASTER_KEY باید دقیقاً ۳۲ بایت (AES-256) باشد");
        }
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
    }

    /** خروجی: [۱۲ بایت IV][متن رمزشده + ۱۶ بایت TAG] */
    public byte[] encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);                       // IV یکتا برای هر رمزنگاری
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey,
                        new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv).put(ciphertext).array();
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("رمزنگاری Secret ناموفق بود", e);
        }
    }

    public String decrypt(byte[] encrypted) {
        if (encrypted == null) return null;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey,
                        new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("رمزگشایی Secret ناموفق بود", e);
        }
    }
}
```

**چرا GCM؟** رمزنگاری + احراز اصالت با هم (AEAD). اگر کسی بایت‌های رمزشده را دستکاری کند، رمزگشایی **شکست می‌خورد** — نه اینکه داده خراب برگرداند.

**نکته مهم:** IV برای هر رمزنگاری **تصادفی و یکتا** است. استفاده مجدد از IV در GCM فاجعه امنیتی است (نشت کلید).

### اتصال به JPA

```java
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, byte[]> {

    private static SecretEncryptor encryptor;   // تزریق از طریق Setter در Config

    @Override public byte[] convertToDatabaseColumn(String attribute) {
        return encryptor.encrypt(attribute);
    }
    @Override public String convertToEntityAttribute(byte[] dbData) {
        return encryptor.decrypt(dbData);
    }
    public static void setEncryptor(SecretEncryptor e) { encryptor = e; }
}
```

```java
@Column(name = "api_key_encrypted", columnDefinition = "bytea")
@Convert(converter = EncryptedStringConverter.class)
private String apiKey;
```

### قواعد مطلق Secret

| ❌ هرگز | ✅ به‌جای آن |
|---|---|
| لاگ کلید | `log.info("Provider {} configured, key hint {}", code, hint)` |
| کلید در DTO پاسخ | فقط `apiKeyHint` (۴ کاراکتر آخر) |
| کلید در Swagger مثال | `@Schema(accessMode = READ_ONLY)` روی فیلد |
| کلید در Stack Trace | `toString()` دستی روی Entity |
| کلید در Git | `.gitignore` + `gitleaks` در CI |
| کلید در AuditLog | فیلد `after_state` قبل از ثبت پاک‌سازی می‌شود |
| کلید در Exception | `EncryptionException` پیام عمومی دارد |

```java
@Entity
public class AiProvider {
    @Override
    public String toString() {
        return "AiProvider{code=" + code + ", enabled=" + enabled +
               ", apiKey=[PROTECTED]}";     // هرگز مقدار واقعی
    }
}
```

### تست امنیتی الزامی

```java
@Test
void api_key_never_appears_in_serialized_output() {
    AiProvider p = providerWithKey("sk-ant-super-secret-value-1234");
    String json = objectMapper.writeValueAsString(providerMapper.toResponse(p));
    assertThat(json).doesNotContain("sk-ant-super-secret-value-1234");
    assertThat(json).contains("1234");     // فقط hint
}

@Test
void api_key_is_encrypted_at_rest() {
    AiProvider saved = repo.save(providerWithKey("sk-secret"));
    String raw = jdbcTemplate.queryForObject(
        "SELECT encode(api_key_encrypted,'escape') FROM ai_providers WHERE id=?",
        String.class, saved.getId());
    assertThat(raw).doesNotContain("sk-secret");
}
```

---

## ۸. ضد-IDOR (بخش ۵۶)

```mermaid
flowchart TD
    A["درخواست GET /api/v1/students/{id}"] --> B{JWT معتبر؟}
    B -->|خیر| E1["401"]
    B -->|بله| C["TenantContext از JWT"]
    C --> D["repo.findById(id)"]
    D --> F{رکورد وجود دارد؟}
    F -->|خیر| E2["404"]
    F -->|بله| G{tenant_id رکورد == tenant_id جاری؟}
    G -->|خیر| H["ثبت IDOR در AuditLog<br/>بازگشت 404"]
    G -->|بله| I{نقش اجازه دارد؟}
    I -->|خیر| E3["403"]
    I -->|بله| J["بازگشت DTO"]
```

### سه قاعده آهنین

1. **شناسه مستأجر هرگز از ورودی کاربر گرفته نمی‌شود.** فقط از JWT.
2. **همه بررسی‌ها در لایه Service** — نه Controller، نه Filter. Controller قابل دور زدن است (مثلاً از طریق فراخوانی داخلی).
3. **خطای «پیدا نشد» و «دسترسی ندارم» از بیرون یکسان‌اند (404).** نشت وجود رکورد ممنوع.

---

## ۹. محدودیت نرخ (Rate Limiting)

| مسیر | محدودیت | کلید |
|---|---|---|
| `/auth/login` | ۱۰ / دقیقه | IP + Username |
| `/auth/eitaa` | ۲۰ / دقیقه | IP |
| `/assignments/*/evaluate` | ۳۰ / دقیقه | کاربر |
| `/ai/**` | طبق سقف پلن | مستأجر |
| سایر APIها | ۶۰۰ / دقیقه | مستأجر |
| `/public/**` | ۶۰ / دقیقه | IP |

پیاده‌سازی فاز اول: `Bucket4j` درون‌حافظه‌ای با `ConcurrentHashMap` (تک‌نمونه‌ای).
پیاده‌سازی فاز ۱۲: Bucket4j روی Redis (چندنمونه‌ای).

---

## ۱۰. اعتبارسنجی ورودی

```java
public record CreateStudentRequest(
    @NotBlank @Size(min = 2, max = 100) String fullName,
    @Pattern(regexp = "^\\d{10}$", message = "کد ملی باید ۱۰ رقم باشد") String nationalCode,
    @Pattern(regexp = "^09\\d{9}$", message = "شماره موبایل معتبر نیست") String guardianMobile,
    @Past LocalDate birthDate,
    @NotNull UUID classroomId
) {}
```

**قواعد:**
- `@Valid` روی **همه** `@RequestBody`.
- `@Validated` روی کلاس Controller برای اعتبارسنجی `@PathVariable`.
- `@Size(max=...)` روی **همه** رشته‌ها (ضد DoS با payload بزرگ).
- محدودیت حجم درخواست در Nginx و Spring (`spring.servlet.multipart.max-file-size`).
- **هرگز** ورودی خام در JPQL الحاق نمی‌شود — همیشه Parameterized (JPA به‌طور پیش‌فرض).

---

## ۱۱. ضد تزریق Prompt (Prompt Injection)

ورودی دانش‌آموز (`answer_text`) داده کاربر است، نه دستور.

```java
public record PromptPayload(
    String systemPrompt,      // فقط از PromptVersion — هرگز از کاربر
    String userPrompt         // شامل ورودی کاربر
) {}
```

**کنترل‌ها:**
1. **جداسازی سخت:** دستورهای سیستمی همیشه در `system` و ورودی کاربر در `user`. هرگز ترکیب نمی‌شوند.
2. **Delimiter:** ورودی کاربر داخل `<student_answer>...</student_answer>` قرار می‌گیرد.
3. **خروجی Structured:** نتیجه با JSON Schema اعتبارسنجی می‌شود. اگر AI خارج از Schema پاسخ دهد → رد، نه ذخیره.
4. **عدم اجرای دستور:** AI هیچ ابزار اجرایی ندارد. فقط متن تولید می‌کند.
5. **بازبینی انسانی:** بخش ۴۰ خواسته‌ها — نمره AI پیشنهاد است، نه حکم قطعی.

```java
private String wrapUntrusted(String userInput) {
    String cleaned = userInput
        .replaceAll("(?i)</?student_answer>", "")   // حذف تلاش برای شکستن delimiter
        .replaceAll("\\p{Cntrl}", " ");              // حذف کاراکترهای کنترلی
    return "<student_answer>\n" + cleaned + "\n</student_answer>";
}
```

---

## ۱۲. هدرهای امنیتی

```java
http.headers(h -> h
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; " +
        "script-src 'self'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data: blob:; " +
        "connect-src 'self'; " +
        "frame-ancestors 'self' https://eitaa.com https://*.eitaa.com; " +
        "object-src 'none'; base-uri 'self'"))
    .frameOptions(f -> f.deny())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true).maxAgeInSeconds(31536000))
    .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .permissionsPolicy(p -> p.policy("camera=(), microphone=(), geolocation=()"))
);
```

> `frame-ancestors` **باید** دامنه‌های ایتا را مجاز کند، چون Mini App داخل WebView ایتا اجرا می‌شود. مقدار دقیق دامنه‌ها بعد از تأیید مستندات ایتا نهایی می‌شود.

### ۱۲.۱ CORS — یک کنترل امنیتی، نه تنظیم راحتی

Mini App از مبدأیی غیر از API اجرا می‌شود: در توسعه `localhost:5173` → `localhost:8080`، و
در WebView ایتا از مبدأ ایتا به مبدأ ما. مرورگر پیش از چنین درخواستی `OPTIONS`
می‌فرستد؛ بدون پاسخ درست، تماس **هرگز فرستاده نمی‌شود** و خطا **هیچ ردی در لاگ سرور
نمی‌گذارد**.

```yaml
app:
  cors:
    allowed-origins: []        # 🔴 پیش‌فرض خالی = هیچ مبدأیی مجاز نیست
```

| قاعده | دلیل |
|---|---|
| پیش‌فرض **خالی** | یک CORS باز یعنی هر سایتی می‌تواند از مرورگر کاربرِ وارد‌شده به API ما درخواست بزند. فراموشی نباید به درِ باز تبدیل شود. |
| **بدون wildcard** | `*` فقط در حالت بدون کوکی مجاز است و اجازه‌دادنش اشتباهی رایج و پرهزینه است. |
| `allowCredentials=false` | برنامک با هدر `Authorization` و `credentials:'omit'` کار می‌کند. اگر پنل مدیریت هم از مبدأ دیگری سرو شود، آن مسیر کوکی HttpOnly دارد و این پرچم باید `true` شود — **یک تصمیم امنیتی، نه یک تنظیم ساده**. |
| فقط `/api/**` | هر مسیری که ثبت شود از مرورگر قابل فراخوانی است؛ `actuator` و دارایی‌های استاتیک بیرون می‌مانند. |
| هدرهای فهرست‌بسته | `Authorization`, `Content-Type`, `Accept`, `X-XSRF-TOKEN`, `X-Requested-With` |

**پیاده‌سازی:** `config/CorsProperties.java` + `security/CorsConfig.java`؛ در
`SecurityConfig` هر دو زنجیره `.cors(withDefaults())` دارند.

> ⚠️ **چرا `@CrossOrigin` روی کنترلر کافی نیست؟** چون بعد از زنجیره فیلتر امنیت اجرا
> می‌شود — یعنی وقتی preflight پیش‌تر با ۴۰۱ رد شده باشد. نتیجه‌اش این است که آزمون
> یکپارچگی با مرورگر هرگز تماس واقعی نمی‌فرستد و خطا هم گمراه‌کننده است.

> ⚠️ **پروفایل `dev`** دو مبدأ را باز می‌کند (`http://localhost:5173` و
> `http://127.0.0.1:5173`)؛ مرورگر این دو را **دو مبدأ متفاوت** می‌بیند. پروفایل
> `prod` هیچ پیش‌فرضی ندارد و باید صریحاً تنظیم شود.

---

## ۱۳. مدیریت Secretها در محیط

```yaml
security:
  encryption:
    master-key: ${ENCRYPTION_MASTER_KEY}     # ۳۲ بایت Base64
  jwt:
    secret: ${JWT_SECRET}                    # حداقل ۳۲ بایت تصادفی
    access-token-ttl: 15m
    refresh-token-ttl: 30d

app:
  eitaa:
    # ⚠️ مسیر کلید درست همین است: `app.eitaa.bot-token` — نه `eitaa.bot.token`.
    # نام متغیر canonical هم `APP_EITAA_BOT_TOKEN` است (relaxed binding).
    bot-token: ${APP_EITAA_BOT_TOKEN:${EITAA_BOT_TOKEN:}}
```

**قواعد:**
- در `application-prod.yml` **هیچ مقدار پیش‌فرضی** وجود ندارد — برنامه در نبود متغیر محیطی **باید fail کند**، نه اینکه با مقدار پیش‌فرض ناامن بالا بیاید.
- ⚠️ **یک استثنای عمدی:** بلوک `app.eitaa` با پیش‌فرض **خالی** نوشته شده
  (`${APP_EITAA_BOT_TOKEN:${EITAA_BOT_TOKEN:}}`). این «مقدار پیش‌فرض» نیست،
  بلکه نگاشت دو نام به یک کلید است: نبودِ هر دو متغیر یعنی توکن
  تنظیم‌نشده — همان رفتار قبل. ⚠️ ولی اینجا برنامه fail **نمی‌کند**
  (فقط `log.warn`)؛ برای جزئیات `EitaaProperties` را ببینید. پیش‌تر
  نگاشت نام در prod نبود و `EITAA_BOT_TOKEN` بی‌صدا نادیده گرفته می‌شد.
- در `application-dev.yml` مقادیر نمونه امن برای توسعه هستند.
- `scripts/generate-keys.sh` برای تولید کلیدها.
- `.env` در `.gitignore` و `.env.example` مستند (بدون مقدار واقعی).

```java
@ConfigurationProperties("security.encryption")
@Validated
public record EncryptionProperties(
    @NotBlank @Size(min = 44, max = 44) String masterKey
) {}
```

---

## ۱۴. لاگ و حسابرسی امنیتی

### هرگز لاگ نشود
`password`, `apiKey`, `token`, `authorization`, `cookie`, `initData`, `secret`

```java
@Component
public class SensitiveDataMasker {
    private static final Pattern[] PATTERNS = {
        Pattern.compile("(\"?(?:password|apiKey|api_key|token|secret|authorization)\"?\\s*[:=]\\s*)\"?([^\",\\s}]+)\"?"),
        Pattern.compile("(Bearer\\s+)[A-Za-z0-9._\\-]+"),
    };
    public String mask(String message) { /* جایگزینی با *** */ }
}
```

متصل به Logback Encoder — **در سطح زیرساخت لاگ**، نه در کد هر سرویس. این تنها راهی است که فراموش نمی‌شود.

### رویدادهای الزامی AuditLog

```text
AUTH_LOGIN_SUCCESS / AUTH_LOGIN_FAILED / AUTH_LOGOUT
TOKEN_REUSE_DETECTED                      ← CRITICAL
IDOR_ATTEMPT                              ← CRITICAL
PROVIDER_CREATED / PROVIDER_UPDATED / PROVIDER_API_KEY_CHANGED  ← CRITICAL
PROMPT_CREATED / PROMPT_PUBLISHED / PROMPT_ROLLED_BACK
PLAN_CREATED / PLAN_PRICE_CHANGED         ← CRITICAL
SUBSCRIPTION_CREATED / CHANGED / CANCELLED
PAYMENT_INITIATED / PAYMENT_VERIFIED / PAYMENT_FAILED / PAYMENT_REFUNDED
USER_ROLE_CHANGED                         ← CRITICAL
USER_DISABLED / USER_ENABLED
USAGE_LIMIT_OVERRIDDEN
SYSTEM_BYPASS_TENANT                      ← CRITICAL
FEATURE_FLAG_CHANGED
```

`before_state` و `after_state` به‌صورت JSONB ذخیره می‌شوند (با پاک‌سازی Secretها).

---

## ۱۵. تست‌های امنیتی الزامی (بخش ۶۰)

فایل: `backend/src/test/java/ir/omid/smartteacher/security/SecurityRequirementsTest.java`

| # | تست | نوع |
|---|---|---|
| 1 | `tenantA_cannotReadTenantB_resource` | Integration |
| 2 | `teacherA_cannotReadTeacherB_students` | Integration |
| 3 | `teacher_cannotModifyGlobalProviderConfig` | Integration |
| 4 | `student_cannotAccessTeacherEndpoints` | Integration |
| 5 | `expiredSubscription_blocksPremiumFeature` | Integration |
| 6 | `usageLimit_isEnforcedAtomicUnderConcurrency` | Integration |
| 7 | `apiKeys_neverLeak_inAnyResponse` | Integration + ArchUnit |
| 8 | `eitaaInitData_withTamperedHash_isRejected` | Unit |
| 9 | `eitaaInitData_withOldAuthDate_isRejected` | Unit |
| 10 | `idor_returns404_notForbidden` | Integration |
| 11 | `refreshToken_reuse_revokesEntireFamily` | Integration |
| 12 | `jwt_withWrongSignature_isRejected` | Unit |
| 13 | `jwt_withAdminRole_inBodyIsIgnored` | Integration |
| 14 | `noNativeQuery_inDomainModules` | ArchUnit |
| 15 | `allMutatingEndpoints_havePreAuthorize` | ArchUnit |
| 16 | `sensitiveFields_maskedInLogs` | Unit |
| 17 | `promptInjection_delimitersAreEscaped` | Unit |
| 18 | `encryptedConverter_roundTrip_andTamperDetection` | Unit |

هر ۱۸ مورد باید در CI سبز باشند. **هیچ PR بدون این‌ها Merge نمی‌شود.**

---

## ۱۶. چک‌لیست پیش از Production (فاز ۱۲)

- [x] تمام Secretها از Env — هیچ مقدار پیش‌فرضی در `prod` (`application-prod.yml`)
- [x] `ddl-auto: validate` (هرگز `update` در prod)
- [x] Swagger در `prod` غیرفعال یا پشت احراز هویت
- [x] Actuator فقط `health,info` (و `prometheus` در prod)؛ `/env` و `/configprops` بسته؛ `info.env/java/os/git` خاموش
- [ ] HTTPS اجباری + HSTS
- [ ] پشتیبان‌گیری رمزنگاری‌شده خودکار + تست بازیابی
- [ ] اسکن آسیب‌پذیری وابستگی‌ها (OWASP Dependency-Check) در CI
- [ ] اسکن Secret در Git (`gitleaks`) — job اختیاری در CI هست؛ هنوز اجباری/سبزِ سخت نیست
- [ ] 2FA اجباری برای `SUPER_ADMIN`
- [ ] بازبینی دسترسی نقش‌ها
- [ ] Rate Limiting فعال روی همه مسیرهای حساس — *پیاده: `/auth/login` (InMemory/Redis)؛ سایر مسیرها هنوز خیر*
- [x] Circuit Breaker in-memory برای Providerهای AI (`ProviderCircuitBreaker` + failover)
- [x] کش TTL in-memory برای `PromptResolver` و `EntitlementService` (Redis stub وقتی `app.redis.enabled=true`)
- [x] اسکریپت smoke بار (`scripts/load-test/k6-smoke.js`) + `docker-compose.observability.yml` برای Prometheus/Grafana
- [ ] تست نفوذ (حداقل یک دور)
- [ ] Log بدون داده حساس
- [ ] رویه چرخش کلید رمزنگاری مستند و آزمایش‌شده
