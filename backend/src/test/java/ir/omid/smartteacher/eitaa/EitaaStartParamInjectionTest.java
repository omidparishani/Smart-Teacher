package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code start_param} داده غیرقابل‌اعتماد است.
 *
 * <p><b>🔴 چرا این کلاس وجود دارد؟</b> {@code start_param} تنها فیلد
 * داخل {@code initData} است که <i>کاربر</i> محتوایش را تعیین می‌کند.
 * هر چیز دیگر را ایتا می‌سازد؛ این را کاربر در لینک Deep Link می‌گذارد
 * و بعد امضا می‌شود — یعنی «امضا خورد» <b>نمی‌گوید</b> «بی‌خطر است».
 * امضا فقط می‌گوید این رشته از ایتا آمده و دست نخورده، نه اینکه محتوایش
 * قابل اعتماد است.
 *
 * <p>پس خطر واقعی این است که کسی این رشته را به‌عنوان <i>دستور</i> به
 * کار ببرد: مسیر فایل، نام کلاس، پرس‌وجوی SQL، آدرس بازگشت. سند
 * ({@code docs/eitaa.md} بخش ۱۲) تنها کاربرد مجاز را «پارس شناسه عددی
 * با بررسی قالب» می‌داند.
 *
 * <p>این کلاس ثابت می‌کند لایه اعتبارسنجی، این رشته را <b>فقط حمل
 * می‌کند</b>. هیچ تفسیری، هیچ اجرایی، هیچ خطایی. اگر روزی کسی در همین
 * لایه وسوسه شود «چون اینجا داریمش، همین‌جا هم تفسیرش کنیم»، این آزمون‌ها
 * می‌افتند.
 */
class EitaaStartParamInjectionTest {

    private static final long USER_ID = 6_100_200_300L;

    private static EitaaInitDataValidator validator() {
        EitaaProperties properties = EitaaTestData.botProperties();
        return new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());
    }

    /** اعتبارسنجی با یک {@code start_param} دلخواه (URL-encoded). */
    private static EitaaInitData validateWithStartParam(String rawStartParam) {
        Map<String, String> params = EitaaTestData.baseParams(USER_ID);
        params.put("start_param", EitaaTestData.encode(rawStartParam));
        return validator().validate(EitaaTestData.signed(params));
    }

    // ─────────────────────────── عبور بی‌تغییر ───────────────────────────

    @Test
    @DisplayName("مسیر فایل، هیچ فایلی را باز نمی‌کند")
    void pathTraversalIsInert() {
        // ⚠️ اگر کسی این رشته را به Files.read یا ClassLoader بدهد،
        // یا استثنایی می‌خوریم یا — بدتر — فایلی خوانده می‌شود.
        // اینجا فقط باید به‌صورت یک رشته بی‌اثر برگردد.
        String traversal = "../../../../etc/passwd";

        EitaaInitData initData = validateWithStartParam(traversal);

        assertThat(initData.startParam())
                .as("رشته باید عیناً برگردد، بدون تفسیر")
                .isEqualTo(traversal);
    }

    @Test
    @DisplayName("رشته شبیه SQL، هیچ پرس‌وجویی اجرا نمی‌کند")
    void sqlLookingValueIsInert() {
        String sql = "'; DROP TABLE users; --";

        assertThat(validateWithStartParam(sql).startParam()).isEqualTo(sql);
    }

    @Test
    @DisplayName("نام کلاس، هیچ کلاسی را بار نمی‌کند")
    void classNameIsNotLoaded() {
        // ⚠️ `java.lang.Runtime` تنها یک رشته است. اگر روزی کسی
        // Class.forName روی این بگذارد، این آزمون همچنان سبز می‌ماند
        // (بار کردن Runtime بی‌خطر است) — ولی assertThatCode اینجا
        // تضمین می‌کند حداقل مسیر فعلی هیچ استثنایی نمی‌سازد.
        assertThatCode(() -> validateWithStartParam("java.lang.Runtime"))
                .doesNotThrowAnyException();

        assertThat(validateWithStartParam("java.lang.Runtime").startParam())
                .isEqualTo("java.lang.Runtime");
    }

    @Test
    @DisplayName("آدرس بازگشت، هیچ ریدایرکتی نمی‌سازد")
    void urlIsCarriedNotFollowed() {
        // ⚠️ الگوی رایج Open Redirect: مقدار Deep Link را در پاسخ
        // «برو به این آدرس» بگذاریم. اینجا فقط حمل می‌شود.
        String url = "https://evil.example.com/steal";

        assertThat(validateWithStartParam(url).startParam()).isEqualTo(url);
    }

    // ─────────────────────────── پیشوندهای Deep Link ───────────────────────────

    @Test
    @DisplayName("پیشوندهای شناخته‌شده دست‌نخورده می‌مانند")
    void knownPrefixesSurviveIntact() {
        // این‌ها تنها قالب‌های مجاز پروژه‌اند
        // (`asg_` تمرین، `cls_` کلاس، `exm_` آزمون).
        for (String value : new String[]{"asg_1024", "cls_55", "exm_7"}) {
            assertThat(validateWithStartParam(value).startParam())
                    .as("مقدار %s باید عیناً بماند", value)
                    .isEqualTo(value);
        }
    }

    // ─────────────────────────── لبه‌ها ───────────────────────────

    @Test
    @DisplayName("مقدار خالی و فاصله، null می‌دهد")
    void blankBecomesNull() {
        // ⚠️ نرمال‌سازی به null مهم است: اگر رشته خالی بماند، هر
        // مصرف‌کننده باید دوباره isEmpty را بررسی کند — و یکی از آن‌ها
        // فراموش می‌کند.
        assertThat(validateWithStartParam("").startParam()).isNull();

        Map<String, String> blankParams = EitaaTestData.baseParams(USER_ID);
        blankParams.put("start_param", "   ");
        assertThat(validator().validate(EitaaTestData.signed(blankParams)).startParam())
                .isNull();
    }

    @Test
    @DisplayName("نبودن start_param یعنی null، نه خطا")
    void absentStartParamIsNull() {
        EitaaInitData initData =
                validator().validate(EitaaTestData.validInitData(USER_ID));

        assertThat(initData.startParam()).isNull();
        assertThat(initData.hasStartParam()).isFalse();
    }

    @Test
    @DisplayName("مقدار با = و & پس از رمزگشایی سالم می‌ماند")
    void valueWithSeparatorsSurvives() {
        // 🔴 اگر پارس پرس‌وجو روی آخرین `=` تقسیم می‌کرد یا مقدار را
        // رمزگشایی‌نشده برمی‌گرداند، اینجا می‌افتاد. مقدار باید پس از
        // عبور از پارسر، دقیقاً همان چیزی باشد که کاربر گذاشته.
        String tricky = "asg_1&cls_2=exm_3";

        Map<String, String> params = EitaaTestData.baseParams(USER_ID);
        params.put("start_param", EitaaTestData.encode(tricky));

        EitaaInitData initData = validator().validate(EitaaTestData.signed(params));

        assertThat(initData.startParam()).isEqualTo(tricky);
        assertThat(initData.queryId()).isNotNull();
    }

    @Test
    @DisplayName("مقدار بسیار بلند، خطا نمی‌دهد و کوتاه نمی‌شود")
    void veryLongValueIsCarriedWhole() {
        // ⚠️ سقف طول در DTO ورودی است (EitaaLoginRequest)، نه اینجا.
        // اینجا نباید برش بخورد: کوتاه‌کردن بی‌صدا، شناسه را خراب
        // می‌کند و باگی می‌سازد که سخت پیدا می‌شود.
        String longValue = "asg_" + "9".repeat(2000);

        assertThat(validateWithStartParam(longValue).startParam())
                .isEqualTo(longValue)
                .hasSize(longValue.length());
    }

    // ─────────────────────────── نشت در Log ───────────────────────────

    @Test
    @DisplayName("toString نمایش شروع پارامتر را لو نمی‌دهد")
    void toStringDoesNotLeakStartParam() {
        // ⚠️ اگر مقدار در Log بیفتد، هر کسی با دسترسی به Log می‌فهمد
        // کاربر از کدام تمرین آمده — داده‌ای که خودش شاید خصوصی باشد.
        String secret = "asg_teacher-private-assignment";

        EitaaInitData initData = validateWithStartParam(secret);

        assertThat(initData.toString())
                .as("وجود داشتن پارامتر ثبت می‌شود، ولی مقدارش نه")
                .contains("hasStartParam=true")
                .doesNotContain(secret);
    }

    @Test
    @DisplayName("toString اثر انگشت ضدبازپخش را لو نمی‌دهد")
    void toStringDoesNotLeakFingerprint() {
        // 🔴 اثر انگشت، هش رشته canonical است. کسی که آن را ببیند
        // نمی‌تواند مستقیماً بازپخش کند، ولی می‌تواند تشخیص دهد کدام
        // initData کدام است — و همین برای ردیابی کافی است.
        EitaaInitData initData =
                validator().validate(EitaaTestData.validInitData(USER_ID));

        assertThat(initData.fingerprint()).isNotBlank();
        assertThat(initData.toString()).doesNotContain(initData.fingerprint());
    }
}
