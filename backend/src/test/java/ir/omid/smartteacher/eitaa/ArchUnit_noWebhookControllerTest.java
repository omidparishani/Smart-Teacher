package ir.omid.smartteacher.eitaa;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * هیچ مسیر ورودی از ایتا وجود ندارد — نه Controller، نه Webhook.
 *
 * <p><b>🔴 چرا این آزمون لازم است؟</b> ایتا هیچ API ای برای دریافت پیام
 * ندارد: نه {@code getUpdates}، نه {@code setWebhook}، نه Long-Polling.
 * ربات ایتا یک کانال <b>یک‌طرفه</b> است ({@code docs/eitaa.md} بخش ۱ و
 * ۸). این یک محدودیت واقعی سرویس است، نه انتخاب ما.
 *
 * <p>خطر اینجاست که کسی — با عادت از تلگرام — کد دریافت پیام اضافه کند:
 * یک {@code @PostMapping("/webhook")}، یک {@code getUpdates}، یا یک متد
 * {@code handleUpdate} روی دروازه. چنین کدی <b>هرگز کار نمی‌کند</b>
 * (چون سرویس آن را ندارد) ولی کامپایل می‌شود، آزمون واحد سبز می‌گیرد،
 * و در Production بی‌صدا هیچ‌وقت صدا زده نمی‌شود. هزینه‌اش این است که
 * تصور «سیستم پیام‌ها را دریافت می‌کند» در طراحی جا می‌افتد و روی آن
 * تصمیم‌های بعدی ساخته می‌شود.
 *
 * <p>پس قاعده اینجا ایستا و سخت‌گیرانه است: اگر روزی ایتا مسیر ورودی
 * داد، این آزمون می‌افتد و کسی باید <i>عمداً</i> بیاید و این فایل را
 * عوض کند — همراه با ثبت آن در {@code docs/eitaa.md}. هدف، جلوگیری از
 * افزودن کد نیست؛ هدف، جلوگیری از افزودن <i>بی‌صدا</i>ی آن است.
 *
 * <p><b>⚠️ چرا بخشی از بررسی‌ها با حلقه نوشته شده و نه با DSL؟</b>
 * قاعده‌های «نام متد» و «مسیر نگاشت» به داده‌ای نیاز دارند که DSL
 * مستقیمی برایش نیست (مقدار داخل {@code @PostMapping}). حلقه صریح،
 * علاوه بر اینکه قابل‌اتکاتر است، پیام شکست روشن‌تری هم می‌دهد:
 * نام کلاس و متد خاطی را می‌گوید، نه فقط «یک قاعده رد شد».
 */
class ArchUnit_noWebhookControllerTest {

    /**
     * قطعه‌نام‌هایی که وجودشان یعنی کسی دنبال دریافت پیام رفته است.
     *
     * <p>همه با حروف کوچک نوشته شده‌اند چون سنجش بدون حساسیت به
     * بزرگی و کوچکی حروف انجام می‌شود.
     */
    private static final List<String> FORBIDDEN_METHOD_NAME_FRAGMENTS = List.of(
            "webhook",        // setWebhook، getWebhookInfo، ...
            "getupdates",     // متد استاندارد دریافت پیام در تلگرام
            "onupdate",       // الگوی شنوند‌ه‌محور
            "handleupdate",
            "processupdate",
            "receiveupdate",
            "receivemessage",
            "pollupdate",     // Long-Polling
            "longpoll");

    /** قطعه‌مسیرهایی که وجودشان در یک Endpoint یعنی مسیر ورودی. */
    private static final List<String> FORBIDDEN_PATH_FRAGMENTS = List.of(
            "webhook",
            "updates",
            "receive");

    /**
     * تنها API دروازه ایتا — مسیر <b>فقط ارسال</b>.
     *
     * <p>⚠️ این فهرست عمداً دقیق است و نه «حداقل». اگر کسی متدی به
     * {@link EitaaAdapter} اضافه کند، آزمون می‌افتد تا او بیاید اینجا و
     * صریحاً بنویسد چه اضافه کرده و چرا — و تأیید کند که متد جدید،
     * ورودی نیست.
     */
    private static final Set<String> OUTBOUND_ONLY_API = Set.of(
            "getBotInfo",
            "sendToChat",
            "sendFileToChat",
            "sendToUser",
            "notifyUser",
            "capabilities",
            "checkHealth");

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        // ⚠️ کلاس‌های تست وارد نمی‌شوند: این آزمون درباره کد production
        // است، و ابزارهای آزمون (مثل سرور محلی) نباید این قواعد را
        // بشکنند.
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ir.omid.smartteacher");
    }

    // ─────────────────────────── نام‌های ورودی ───────────────────────────

    @Test
    @DisplayName("هیچ متدی نام API دریافت پیام را ندارد")
    void noMethodLooksLikeAnInboundApi() {
        List<String> offenders = new ArrayList<>();

        for (JavaClass javaClass : productionClasses) {
            for (JavaMethod method : javaClass.getMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);

                for (String fragment : FORBIDDEN_METHOD_NAME_FRAGMENTS) {
                    if (name.contains(fragment)) {
                        offenders.add(javaClass.getName() + "#" + method.getName()
                                + " (شبیه «" + fragment + "»)");
                    }
                }
            }
        }

        assertThat(offenders)
                .as("ایتا مسیر دریافت پیام ندارد؛ چنین متدی هرگز صدا زده نمی‌شود")
                .isEmpty();

        assertThat(productionClasses)
                .as("اگر بایت‌کدی وارد نشده باشد، این آزمون بی‌معنا است")
                .isNotEmpty();
    }

    @Test
    @DisplayName("هیچ کلاسی نام Webhook ندارد")
    void noClassIsNamedLikeAWebhook() {
        // ⚠️ این یکی با DSL نوشته شده چون قاعده‌های سطح کلاس مستقیم
        // هستند و پیام شکست پیش‌فرض ArchUnit اینجا کافی است.
        ArchRule rule = noClasses()
                .that().resideInAPackage("ir.omid.smartteacher..")
                .should().haveSimpleNameContaining("Webhook")
                .because("ایتا Webhook ندارد؛ کلاسی با این نام یعنی طراحی روی قابلیتی ساختگی");

        rule.check(productionClasses);
    }

    // ─────────────────────────── دروازه ───────────────────────────

    @Test
    @DisplayName("دروازه ایتا فقط متدهای ارسال را دارد")
    void adapterDeclaresOnlyOutboundMethods() {
        Set<String> declared = Arrays.stream(EitaaAdapter.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(declared)
                .as("هر متد تازه روی دروازه باید عمداً به این فهرست اضافه شود")
                .containsExactlyInAnyOrderElementsOf(OUTBOUND_ONLY_API);

        assertThat(declared)
                .as("دروازه نباید هیچ متدی برای دریافت داشته باشد")
                .noneMatch(name -> FORBIDDEN_METHOD_NAME_FRAGMENTS.stream()
                        .anyMatch(fragment -> name.toLowerCase(Locale.ROOT).contains(fragment)));
    }

    // ─────────────────────────── نگاشت‌های وب ───────────────────────────

    @Test
    @DisplayName("هیچ Endpoint ای مسیر ورودی ایتا را نمی‌پذیرد")
    void noEndpointAcceptsAnInboundPath() {
        List<Mapped> mappings = allMappings();

        List<String> offenders = mappings.stream()
                .filter(mapped -> FORBIDDEN_PATH_FRAGMENTS.stream()
                        .anyMatch(fragment -> mapped.path().toLowerCase(Locale.ROOT)
                                .contains(fragment)))
                .map(mapped -> mapped.where() + " → «" + mapped.path() + "»")
                .toList();

        assertThat(offenders)
                .as("هیچ Endpoint ای نباید مسیر دریافت پیام ایتا را باز کند")
                .isEmpty();
    }

    @Test
    @DisplayName("نگاشت‌های وب واقعاً خوانده می‌شوند — آزمون بی‌معنا نیست")
    void mappingScanReadsBothClassAndMethodLevel() {
        // 🔴 این آزمون، آزمون بالایی را معتبر می‌کند. اگر پیمایش نگاشت‌ها
        // خراب شود (مثلاً فقط نگاشت سطح کلاس خوانده شود)، آزمون بالایی
        // روی یک فهرست خالی اجرا می‌شد و همیشه سبز می‌ماند — بی‌آنکه
        // چیزی را بررسی کند. اینجا ثابت می‌کنیم هر دو سطح خوانده
        // می‌شوند: مسیر ورود، سطح کلاس است و بقیه، سطح متد.
        assertThat(allMappings())
                .as("نگاشت سطح کلاس و سطح متد هر دو باید خوانده شوند")
                .extracting(Mapped::path)
                .contains("/api/v1/auth", "/eitaa", "/me", "/refresh", "/logout");
    }

    // ─────────────────────────── پیمایش نگاشت‌ها ───────────────────────────

    /**
     * همه مسیرهای نگاشت‌شده در کد production.
     *
     * <p>⚠️ از {@code getProperties()} استفاده می‌شود و نه
     * {@code getAnnotationOfType}: دومی یک پروکسی می‌سازد که مقدار
     * پیش‌فرض را هم برمی‌گرداند و نمی‌شود فهمید کدام مقدار واقعاً در کد
     * نوشته شده. آنچه اینجا لازم است، مقدار <i>نوشته‌شده</i> است.
     */
    private static List<Mapped> allMappings() {
        List<Mapped> mappings = new ArrayList<>();

        for (JavaClass javaClass : productionClasses) {
            collectMappings(javaClass.getAnnotations(), javaClass.getName(), mappings);

            for (JavaMethod method : javaClass.getMethods()) {
                collectMappings(method.getAnnotations(),
                        javaClass.getName() + "#" + method.getName(), mappings);
            }
        }

        return mappings;
    }

    private static void collectMappings(Iterable<? extends JavaAnnotation<?>> annotations,
                                       String where,
                                       List<Mapped> sink) {
        for (JavaAnnotation<?> annotation : annotations) {
            // ⚠️ سنجش بر پایه پسوند نام است، نه فهرست صریح: نگاشت‌های
            // Spring (GetMapping، PostMapping، ...) همه همین پسوند را
            // دارند و نگاشت تازه‌ای که Spring اضافه کند هم پوشش داده
            // می‌شود.
            if (!annotation.getRawType().getName().endsWith("Mapping")) {
                continue;
            }

            Map<String, Object> properties = annotation.getProperties();

            for (String key : List.of("value", "path")) {
                Object raw = properties.get(key);

                if (raw instanceof Object[] array) {
                    for (Object element : array) {
                        sink.add(new Mapped(where, String.valueOf(element)));
                    }
                } else if (raw != null) {
                    sink.add(new Mapped(where, String.valueOf(raw)));
                }
            }
        }
    }

    /** یک مسیر نگاشت‌شده، همراه با جای دقیقش برای پیام شکست. */
    private record Mapped(String where, String path) {
    }
}
