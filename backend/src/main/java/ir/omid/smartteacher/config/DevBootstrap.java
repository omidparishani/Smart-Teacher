package ir.omid.smartteacher.config;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed توسعه — فقط پروفایل {@code dev}.
 *
 * <p>داده‌های واقع‌گرایانه شامل ادمین، معلم، کلاس‌ها، دانش‌آموزان، کتاب، تکالیف، آزمون،
 * ارائه‌دهندگان هوش مصنوعی، پرامپت‌ها و کدهای تخفیف را مقداردهی می‌کند.
 */
@Component
@Profile("dev")
public class DevBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevBootstrap.class);

    private static final UUID ROLE_SUPER_ADMIN =
            UUID.fromString("10000000-0000-7000-8000-000000000001");

    // شناسه‌های ثابت محیط توسعه برای تضمین تکرارپذیری
    public static final UUID DEV_TEACHER_ID = UUID.fromString("01a10c48-a456-777b-84a9-8860accdd195");
    public static final UUID DEV_TENANT_ID = UUID.fromString("01a10c48-a45f-70f5-8249-ac1d86b042c3");
    public static final long DEV_EITAA_USER_ID = 100001L;

    public static final UUID MATH_SUBJECT_ID = UUID.fromString("01a10c48-b001-7000-8000-000000000001");
    public static final UUID SCIENCE_SUBJECT_ID = UUID.fromString("01a10c48-b001-7000-8000-000000000002");
    public static final UUID PERSIAN_SUBJECT_ID = UUID.fromString("01a10c48-b001-7000-8000-000000000003");

    public static final UUID CLASS_901_ID = UUID.fromString("01a10c48-c001-7000-8000-000000000001");
    public static final UUID CLASS_902_ID = UUID.fromString("01a10c48-c001-7000-8000-000000000002");

    public static final UUID STUDENT_SARA_ID = UUID.fromString("01a10c48-d001-7000-8000-000000000001");
    public static final UUID STUDENT_AMIR_ID = UUID.fromString("01a10c48-d001-7000-8000-000000000002");
    public static final UUID STUDENT_ZAHRA_ID = UUID.fromString("01a10c48-d001-7000-8000-000000000003");
    public static final UUID STUDENT_MEHDI_ID = UUID.fromString("01a10c48-d001-7000-8000-000000000004");
    public static final UUID STUDENT_FATEMEH_ID = UUID.fromString("01a10c48-d001-7000-8000-000000000005");

    public static final UUID MATH_BOOK_ID = UUID.fromString("01a10c48-e001-7000-8000-000000000001");
    public static final UUID CHAPTER_1_ID = UUID.fromString("01a10c48-e002-7000-8000-000000000001");
    public static final UUID CHAPTER_2_ID = UUID.fromString("01a10c48-e002-7000-8000-000000000002");

    public static final UUID ASSIGNMENT_1_ID = UUID.fromString("01a10c48-f001-7000-8000-000000000001");
    public static final UUID SUBMISSION_SARA_ID = UUID.fromString("01a10c48-f002-7000-8000-000000000001");
    public static final UUID SUBMISSION_AMIR_ID = UUID.fromString("01a10c48-f002-7000-8000-000000000002");

    public static final UUID EXAM_1_ID = UUID.fromString("01a10c48-a001-7000-8000-000000000001");

    public static final UUID PROVIDER_MOCK_ID = UUID.fromString("01a10c48-9001-7000-8000-000000000001");
    public static final UUID PROVIDER_OPENAI_ID = UUID.fromString("01a10c48-9001-7000-8000-000000000002");

    public static final UUID MODEL_MOCK_CHAT_ID = UUID.fromString("01a10c48-9002-7000-8000-000000000001");
    public static final UUID MODEL_MOCK_SMART_ID = UUID.fromString("01a10c48-9002-7000-8000-000000000002");
    public static final UUID MODEL_GPT_4O_ID = UUID.fromString("01a10c48-9002-7000-8000-000000000003");
    public static final UUID MODEL_GPT_4O_MINI_ID = UUID.fromString("01a10c48-9002-7000-8000-000000000004");

    public static final UUID COUPON_NOROOZ_ID = UUID.fromString("01a10c48-8001-7000-8000-000000000001");
    public static final UUID COUPON_TEACHER_ID = UUID.fromString("01a10c48-8001-7000-8000-000000000002");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final boolean enabled;

    public DevBootstrap(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            @Value("${app.dev.seed:true}") boolean enabled,
            @Value("${app.dev.admin-username:superadmin}") String adminUsername,
            @Value("${app.dev.admin-password:ChangeMe-Admin-1!}") String adminPassword) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        seedAdmin();
        seedDevTeacherAndTenant();
        seedAiProvidersAndModels();
        seedPrompts();
        seedCoupons();
        seedEducationalData();
    }

    private void seedAdmin() {
        Integer count = jdbc.queryForObject(
                "select count(*) from users where lower(username) = lower(?) and deleted_at is null",
                Integer.class,
                adminUsername);
        if (count != null && count > 0) {
            return;
        }

        UUID userId = UuidV7Generator.generate();
        String hash = passwordEncoder.encode(adminPassword);
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, adminUsername, hash, "مدیر ارشد (توسعه)");
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);

        // 🔴 رمز در Log نمی‌آید — عمدی است.
        //
        // نسخه پیشین `password={}` را چاپ می‌کرد. حتی برای یک رمز seed
        // توسعه‌ای هم این کار غلط است: لاگ‌ها آرشیو، جمع‌آوری و بین
        // ماشین‌ها جابه‌جا می‌شوند، و عادتِ «رمز در لاگ» یک بار که جا
        // بیفتد در جای دیگری هم تکرار می‌شود (قاعده بخش ۵۳ خواسته‌ها:
        // راز نباید در Log، Swagger، حسابرسی یا استثنا ظاهر شود).
        //
        // ⚠️ خودِ رمز گم نشده: مقدارش در `application-dev.yml`
        // (`app.dev.admin-password`، قابل‌بازنویسی با `APP_DEV_ADMIN_PASSWORD`)
        // و در بنر `scripts/run-dev.ps1` است — یعنی جایی که توسعه‌دهنده
        // دنبالش می‌گردد.
        log.info("DevBootstrap: ادمین ساخته شد → username={} (رمز: app.dev.admin-password)", adminUsername);
    }

    private void seedDevTeacherAndTenant() {
        // ۱. اطمینان از وجود کاربر معلم
        Integer teacherCount = jdbc.queryForObject(
                "select count(*) from users where id = ?",
                Integer.class,
                DEV_TEACHER_ID);
        if (teacherCount == null || teacherCount == 0) {
            jdbc.update("""
                    insert into users (id, username, full_name, status)
                    values (?, 'ali_dev', 'Ali Dev', 'ACTIVE')
                    on conflict (id) do nothing
                    """,
                    DEV_TEACHER_ID);
        }

        // ۲. اطمینان از وجود مستأجر معلم
        Integer tenantCount = jdbc.queryForObject(
                "select count(*) from tenants where id = ?",
                Integer.class,
                DEV_TENANT_ID);
        if (tenantCount == null || tenantCount == 0) {
            jdbc.update("""
                    insert into tenants (id, type, name, slug, owner_user_id, status)
                    values (?, 'TEACHER', 'کلاس‌های Ali Dev', 't-dev-ali', ?, 'ACTIVE')
                    on conflict (id) do nothing
                    """,
                    DEV_TENANT_ID, DEV_TEACHER_ID);
        }

        // ۳. عضویت مالک در مستأجر
        jdbc.update("""
                insert into tenant_members (id, tenant_id, user_id, role, status)
                values (?, ?, ?, 'OWNER', 'ACTIVE')
                on conflict (tenant_id, user_id) do nothing
                """,
                UuidV7Generator.generate(), DEV_TENANT_ID, DEV_TEACHER_ID);

        // ۴. پیوند هویت ایتا
        jdbc.update("""
                insert into eitaa_identities (id, user_id, eitaa_user_id, eitaa_username, first_name, last_name, language_code, allows_write_to_pm)
                values (?, ?, ?, 'ali_dev', 'Ali', 'Dev', 'fa', true)
                on conflict (eitaa_user_id) do nothing
                """,
                UuidV7Generator.generate(), DEV_TEACHER_ID, DEV_EITAA_USER_ID);
    }

    private void seedAiProvidersAndModels() {
        // Provider: MOCK
        jdbc.update("""
                insert into ai_providers (id, code, name, type, base_url, enabled)
                values (?, 'MOCK', 'ارائه‌دهنده تستی و شبیه‌ساز', 'MOCK', 'http://localhost:8080', true)
                on conflict (code) do update set enabled = true
                """,
                PROVIDER_MOCK_ID);

        // Provider: OPENAI_COMPATIBLE
        jdbc.update("""
                insert into ai_providers (id, code, name, type, base_url, enabled)
                values (?, 'OPENAI_COMPATIBLE', 'ارائه‌دهنده استاندارد OpenAI / DeepSeek', 'OPENAI_COMPATIBLE', 'https://api.openai.com/v1', true)
                on conflict (code) do update set enabled = true
                """,
                PROVIDER_OPENAI_ID);

        // Models for MOCK
        jdbc.update("""
                insert into ai_models (id, provider_id, code, display_name, enabled, currency)
                values (?, ?, 'mock-chat', 'مدل پیش‌فرض Mock Chat', true, 'USD')
                on conflict (provider_id, code) do nothing
                """,
                MODEL_MOCK_CHAT_ID, PROVIDER_MOCK_ID);

        jdbc.update("""
                insert into ai_models (id, provider_id, code, display_name, enabled, currency)
                values (?, ?, 'mock-smart', 'مدل پیشرفته هوشمند تستی', true, 'USD')
                on conflict (provider_id, code) do nothing
                """,
                MODEL_MOCK_SMART_ID, PROVIDER_MOCK_ID);

        // Models for OPENAI
        jdbc.update("""
                insert into ai_models (id, provider_id, code, display_name, enabled, currency)
                values (?, ?, 'gpt-4o', 'GPT-4o (OpenAI)', true, 'USD')
                on conflict (provider_id, code) do nothing
                """,
                MODEL_GPT_4O_ID, PROVIDER_OPENAI_ID);

        jdbc.update("""
                insert into ai_models (id, provider_id, code, display_name, enabled, currency)
                values (?, ?, 'gpt-4o-mini', 'GPT-4o Mini (سریع و اقتصادی)', true, 'USD')
                on conflict (provider_id, code) do nothing
                """,
                MODEL_GPT_4O_MINI_ID, PROVIDER_OPENAI_ID);

        // تخصیص سراسری (Global Assignment)
        Integer globalAsgCount = jdbc.queryForObject(
                "select count(*) from ai_provider_assignments where scope_type = 'GLOBAL'",
                Integer.class);
        if (globalAsgCount == null || globalAsgCount == 0) {
            jdbc.update("""
                    insert into ai_provider_assignments (id, scope_type, scope_id, provider_id, model_id, priority, enabled)
                    values (?, 'GLOBAL', null, ?, ?, 100, true)
                    """,
                    UuidV7Generator.generate(), PROVIDER_MOCK_ID, MODEL_MOCK_CHAT_ID);
        }
    }

    private void seedPrompts() {
        // پرامپت چت دستیار (اگر نسخه نداشته باشد)
        UUID defaultPromptId = UUID.fromString("a1000000-0000-7000-8000-000000000001");
        Integer chatVersions = jdbc.queryForObject(
                "select count(*) from prompt_versions where prompt_id = ?",
                Integer.class,
                defaultPromptId);
        if (chatVersions == null || chatVersions == 0) {
            jdbc.update("""
                    insert into prompt_versions (id, prompt_id, version_number, system_prompt, user_prompt, variables, status)
                    values (?, ?, 1,
                            'شما دستیار هوشمند و دلسوز معلمان در پیام‌رسان ایتا هستید. پاسخ‌ها را دقیق، محترمانه و به زبان فارسی بنویسید.',
                            '{{query}}',
                            '["query"]'::jsonb,
                            'PUBLISHED')
                    """,
                    UuidV7Generator.generate(), defaultPromptId);
        }

        // پرامپت تولید آزمون
        UUID examPromptId = UUID.fromString("a1000000-0000-7000-8000-000000000002");
        jdbc.update("""
                insert into prompts (id, code, name, feature_code, description, is_system)
                values (?, 'EXAM_GENERATION', 'تولید هوشمند آزمون', 'EXAM_GEN', 'تولید سوالات آزمون با درجه سختی مشخص', true)
                on conflict (code) do nothing
                """,
                examPromptId);

        Integer examPromptVersions = jdbc.queryForObject(
                "select count(*) from prompt_versions where prompt_id = ?",
                Integer.class,
                examPromptId);
        if (examPromptVersions == null || examPromptVersions == 0) {
            jdbc.update("""
                    insert into prompt_versions (id, prompt_id, version_number, system_prompt, user_prompt, variables, status)
                    values (?, ?, 1,
                            'شما طراح آزمون استاندارد در آموزش و پرورش هستید. برای هر سوال گزینه صحیح و بازخورد توضیحی تولید کنید.',
                            'برای مبحث {{topic}} تعداد {{count}} سوال با سطح سختی {{difficulty}} در قالب JSON تولید کن.',
                            '["topic", "count", "difficulty"]'::jsonb,
                            'PUBLISHED')
                    """,
                    UuidV7Generator.generate(), examPromptId);
        }

        // پرامپت تصحیح تکلیف
        UUID hwPromptId = UUID.fromString("a1000000-0000-7000-8000-000000000003");
        jdbc.update("""
                insert into prompts (id, code, name, feature_code, description, is_system)
                values (?, 'HOMEWORK_CORRECTION', 'تصحیح خودکار تکلیف', 'HOMEWORK_EVAL', 'ارزیابی پاسخ دانش‌آموز با هوش مصنوعی', true)
                on conflict (code) do nothing
                """,
                hwPromptId);

        Integer hwPromptVersions = jdbc.queryForObject(
                "select count(*) from prompt_versions where prompt_id = ?",
                Integer.class,
                hwPromptId);
        if (hwPromptVersions == null || hwPromptVersions == 0) {
            jdbc.update("""
                    insert into prompt_versions (id, prompt_id, version_number, system_prompt, user_prompt, variables, status)
                    values (?, ?, 1,
                            'شما مصحح و معلم راهنما هستید. پاسخ دانش‌آموز را با پاسخ معیار مقایسه و نمره مستدل ثبت کنید.',
                            'تکلیف: {{question}}\nپاسخ معیار: {{expected_answer}}\nپاسخ دانش‌آموز: {{student_answer}}\nبارم: {{max_score}}',
                            '["question", "expected_answer", "student_answer", "max_score"]'::jsonb,
                            'PUBLISHED')
                    """,
                    UuidV7Generator.generate(), hwPromptId);
        }
    }

    private void seedCoupons() {
        jdbc.update("""
                insert into coupons (id, code, discount_type, discount_value, min_purchase_amount, valid_from, valid_until, active)
                values (?, 'NOROOZ1405', 'PERCENTAGE', 30.0000, 0, now() - interval '1 day', now() + interval '1 year', true)
                on conflict (code) do nothing
                """,
                COUPON_NOROOZ_ID);

        jdbc.update("""
                insert into coupons (id, code, discount_type, discount_value, min_purchase_amount, valid_from, valid_until, active)
                values (?, 'TEACHER50', 'PERCENTAGE', 50.0000, 0, now() - interval '1 day', now() + interval '1 year', true)
                on conflict (code) do nothing
                """,
                COUPON_TEACHER_ID);
    }

    private void seedEducationalData() {
        // ۱. دروس
        jdbc.update("""
                insert into subjects (id, tenant_id, name, code, grade_level)
                values (?, ?, 'ریاضی نهم', 'MATH-9', 'نهم')
                on conflict (tenant_id, id) do nothing
                """,
                MATH_SUBJECT_ID, DEV_TENANT_ID);

        jdbc.update("""
                insert into subjects (id, tenant_id, name, code, grade_level)
                values (?, ?, 'علوم تجربی نهم', 'SCIENCE-9', 'نهم')
                on conflict (tenant_id, id) do nothing
                """,
                SCIENCE_SUBJECT_ID, DEV_TENANT_ID);

        jdbc.update("""
                insert into subjects (id, tenant_id, name, code, grade_level)
                values (?, ?, 'ادبیات فارسی نهم', 'PERSIAN-9', 'نهم')
                on conflict (tenant_id, id) do nothing
                """,
                PERSIAN_SUBJECT_ID, DEV_TENANT_ID);

        // ۲. کلاس‌ها
        jdbc.update("""
                insert into classrooms (id, tenant_id, name, grade_level, academic_year, subject_id, homeroom_teacher_id, capacity, status)
                values (?, ?, 'کلاس ۹۰۱ - ریاضی نهم', 'نهم', '1404-1405', ?, ?, 32, 'ACTIVE')
                on conflict (tenant_id, id) do nothing
                """,
                CLASS_901_ID, DEV_TENANT_ID, MATH_SUBJECT_ID, DEV_TEACHER_ID);

        jdbc.update("""
                insert into classrooms (id, tenant_id, name, grade_level, academic_year, subject_id, homeroom_teacher_id, capacity, status)
                values (?, ?, 'کلاس ۹۰۲ - علوم تجربی', 'نهم', '1404-1405', ?, ?, 30, 'ACTIVE')
                on conflict (tenant_id, id) do nothing
                """,
                CLASS_902_ID, DEV_TENANT_ID, SCIENCE_SUBJECT_ID, DEV_TEACHER_ID);

        // ۳. دانش‌آموزان
        seedStudent(STUDENT_SARA_ID, "سارا محمدی", "0021345671", "رضا محمدی", "09121111111", "FEMALE");
        seedStudent(STUDENT_AMIR_ID, "امیرحسین رضایی", "0021345672", "محسن رضایی", "09122222222", "MALE");
        seedStudent(STUDENT_ZAHRA_ID, "زهرا کریمی", "0021345673", "احمد کریمی", "09123333333", "FEMALE");
        seedStudent(STUDENT_MEHDI_ID, "مهدی کاظمی", "0021345674", "داوود کاظمی", "09124444444", "MALE");
        seedStudent(STUDENT_FATEMEH_ID, "فاطمه حسینی", "0021345675", "سید علی حسینی", "09125555555", "FEMALE");

        // ۴. ثبت‌نام دانش‌آموزان در کلاس‌ها
        enrollStudent(CLASS_901_ID, STUDENT_SARA_ID, "101");
        enrollStudent(CLASS_901_ID, STUDENT_AMIR_ID, "102");
        enrollStudent(CLASS_901_ID, STUDENT_ZAHRA_ID, "103");
        enrollStudent(CLASS_901_ID, STUDENT_MEHDI_ID, "104");
        enrollStudent(CLASS_901_ID, STUDENT_FATEMEH_ID, "105");

        enrollStudent(CLASS_902_ID, STUDENT_SARA_ID, "201");
        enrollStudent(CLASS_902_ID, STUDENT_AMIR_ID, "202");
        enrollStudent(CLASS_902_ID, STUDENT_ZAHRA_ID, "203");

        // ۵. کتاب درسی
        jdbc.update("""
                insert into books (id, tenant_id, title, publisher, grade_level, subject_id, total_pages, status)
                values (?, ?, 'کتاب ریاضی پایه نهم', 'شرکت چاپ و نشر کتاب‌های درسی ایران', 'نهم', ?, 140, 'READY')
                on conflict (tenant_id, id) do nothing
                """,
                MATH_BOOK_ID, DEV_TENANT_ID, MATH_SUBJECT_ID);

        // فصل‌های کتاب
        jdbc.update("""
                insert into book_chapters (id, tenant_id, book_id, title, order_index, start_page, end_page)
                values (?, ?, ?, 'فصل اول: مجموعه‌ها و احتمال', 1, 1, 25)
                on conflict (tenant_id, id) do nothing
                """,
                CHAPTER_1_ID, DEV_TENANT_ID, MATH_BOOK_ID);

        jdbc.update("""
                insert into book_chapters (id, tenant_id, book_id, title, order_index, start_page, end_page)
                values (?, ?, ?, 'فصل دوم: عددهای حقیقی', 2, 26, 50)
                on conflict (tenant_id, id) do nothing
                """,
                CHAPTER_2_ID, DEV_TENANT_ID, MATH_BOOK_ID);

        // ۶. تکلیف و ارسال پاسخ
        jdbc.update("""
                insert into assignments (id, tenant_id, classroom_id, book_id, page_from, page_to, title, description, type, max_score, status, ai_evaluation_enabled)
                values (?, ?, ?, ?, 10, 14, 'تکلیف تمرین‌های فصل اول (مجموعه‌ها)', 'تمرین‌های ۱ تا ۵ صفحه ۱۲ و تمرین صفحه ۱۴ را حل کنید.', 'HOMEWORK', 20.00, 'PUBLISHED', true)
                on conflict (tenant_id, id) do nothing
                """,
                ASSIGNMENT_1_ID, DEV_TENANT_ID, CLASS_901_ID, MATH_BOOK_ID);

        jdbc.update("""
                insert into assignment_submissions (id, tenant_id, assignment_id, student_id, answer_text, status, final_score, teacher_overrode_ai)
                values (?, ?, ?, ?, 'پاسخ تمرین ۱: A ∩ B = {2, 3}. پاسخ تمرین ۲: {1, 2, 3, 4, 5}. تمرین ۳: تهی زیرمجموعه همه است.', 'AI_EVALUATED', 19.00, false)
                on conflict (tenant_id, id) do nothing
                """,
                SUBMISSION_SARA_ID, DEV_TENANT_ID, ASSIGNMENT_1_ID, STUDENT_SARA_ID);

        jdbc.update("""
                insert into assignment_submissions (id, tenant_id, assignment_id, student_id, answer_text, status, teacher_overrode_ai)
                values (?, ?, ?, ?, 'تمرین‌های ۱ تا ۵ حل شد و پیوست گردید.', 'SUBMITTED', false)
                on conflict (tenant_id, id) do nothing
                """,
                SUBMISSION_AMIR_ID, DEV_TENANT_ID, ASSIGNMENT_1_ID, STUDENT_AMIR_ID);

        // ۷. آزمون کلاسی و سوالات
        jdbc.update("""
                insert into exams (id, tenant_id, classroom_id, book_id, title, description, duration_minutes, total_score, status, show_results_immediately)
                values (?, ?, ?, ?, 'آزمون کلاسی فصل اول ریاضی', 'شامل مباحث مجموعه، زیرمجموعه و احتمال', 45, 20.00, 'ACTIVE', true)
                on conflict (tenant_id, id) do nothing
                """,
                EXAM_1_ID, DEV_TENANT_ID, CLASS_901_ID, MATH_BOOK_ID);

        seedExamQuestion(UUID.fromString("01a10c48-a002-7000-8000-000000000001"), EXAM_1_ID, "MCQ",
                "اگر مجموعه A دارای ۵ عضو باشد، تعداد زیرمجموعه‌های آن چند است؟",
                "[{\"text\": \"16\", \"is_correct\": false}, {\"text\": \"32\", \"is_correct\": true}, {\"text\": \"25\", \"is_correct\": false}, {\"text\": \"64\", \"is_correct\": false}]",
                "32", 5.0, "EASY", 1);

        seedExamQuestion(UUID.fromString("01a10c48-a002-7000-8000-000000000002"), EXAM_1_ID, "MCQ",
                "مجموعه اعداد طبیعی فرد کمتر از ۱۰ کدام است؟",
                "[{\"text\": \"{1, 3, 5, 7, 9}\", \"is_correct\": true}, {\"text\": \"{1, 3, 5, 7}\", \"is_correct\": false}, {\"text\": \"{3, 5, 7, 9}\", \"is_correct\": false}, {\"text\": \"{2, 4, 6, 8}\", \"is_correct\": false}]",
                "{1, 3, 5, 7, 9}", 5.0, "EASY", 2);

        seedExamQuestion(UUID.fromString("01a10c48-a002-7000-8000-000000000003"), EXAM_1_ID, "TRUE_FALSE",
                "مجموعه تهی، زیرمجموعه هر مجموعه‌ای است.",
                "[{\"text\": \"صحیح\", \"is_correct\": true}, {\"text\": \"غلط\", \"is_correct\": false}]",
                "صحیح", 5.0, "EASY", 3);

        seedExamQuestion(UUID.fromString("01a10c48-a002-7000-8000-000000000004"), EXAM_1_ID, "SHORT_ANSWER",
                "تعداد اعضای مجموعه اعداد اول تک‌رقمی را بنویسید.",
                null,
                "4", 5.0, "MEDIUM", 4);

        log.info("DevBootstrap: داده‌های آموزشی نمونه (کلاس‌ها، دانش‌آموزان، کتاب، تکلیف و آزمون) با موفقیت مقداردهی شدند.");
    }

    private void seedStudent(UUID id, String name, String nationalCode, String guardianName, String guardianMobile, String gender) {
        jdbc.update("""
                insert into students (id, tenant_id, full_name, national_code, guardian_name, guardian_mobile, gender, status)
                values (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                on conflict (tenant_id, id) do nothing
                """,
                id, DEV_TENANT_ID, name, nationalCode, guardianName, guardianMobile, gender);
    }

    private void enrollStudent(UUID classId, UUID studentId, String rollNumber) {
        jdbc.update("""
                insert into class_students (id, tenant_id, classroom_id, student_id, roll_number)
                values (?, ?, ?, ?, ?)
                on conflict (tenant_id, classroom_id, student_id) do nothing
                """,
                UuidV7Generator.generate(), DEV_TENANT_ID, classId, studentId, rollNumber);
    }

    private void seedExamQuestion(UUID qId, UUID examId, String type, String text, String optionsJson, String answer, double score, String diff, int order) {
        jdbc.update("""
                insert into exam_questions (id, tenant_id, exam_id, type, question_text, options, correct_answer, score, difficulty, order_index)
                values (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
                on conflict (tenant_id, id) do nothing
                """,
                qId, DEV_TENANT_ID, examId, type, text, optionsJson, answer, score, diff, order);
    }
}
