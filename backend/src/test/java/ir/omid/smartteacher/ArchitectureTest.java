package ir.omid.smartteacher;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * بازبینی معماری با ArchUnit.
 *
 * <p>Spring Modulith مرزهای <i>ماژول</i> را می‌سنجد
 * ({@code ApplicationModulesTest}). این کلاس چیزهایی را می‌سنجد که
 * Modulith کاری با آن‌ها ندارد: قواعد سبک کدنویسی و الگوهای امنیتی که
 * با گذشت زمان بی‌صدا از بین می‌روند.
 *
 * <p><b>⚠️ چرا هر قاعده اینجاست؟</b> هرکدام شکست یک تصمیم طراحی را
 * ثبت می‌کند که گرفتنش در Code Review دشوار است. یک قاعده بدون این
 * «چرا» بعداً حذف می‌شود چون کسی دلیلش را نمی‌داند.
 *
 * <p><b>⚠️ کلاس‌های تست وارد نمی‌شوند</b> ({@code DO_NOT_INCLUDE_TESTS}).
 * این قواعد برای کد production نوشته شده‌اند؛ مثلاً تزریق در فیلد در
 * کلاس‌های تست ایرادی ندارد و نباید این مجموعه را بشکند.
 */
class ArchitectureTest {

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ir.omid.smartteacher");
    }

    // ─────────────────────── امنیت ───────────────────────

    /**
     * ⚠️ دلیل: تزریق در فیلد، وابستگی را پنهان می‌کند و اجازه می‌دهد
     * شیء در وضعیت نیمه‌ساخته استفاده شود. بدتر، تست‌نویسی را به
     * Reflection وادار می‌کند و همین باعث می‌شود تست‌های امنیتی حساس
     * (مثل جداسازی مستأجر) دیرتر و کم‌دقت‌تر نوشته شوند. همه کلاس‌های
     * این پروژه تزریق از سازنده دارند.
     */
    @Test
    @DisplayName("هیچ فیلدی تزریق وابستگی ندارد")
    void noFieldInjection() {
        ArchRule rule = noFields()
                .should().beAnnotatedWith(Autowired.class)
                .because("تزریق باید از سازنده باشد تا وابستگی‌ها صریح و تست‌پذیر بمانند");

        rule.check(productionClasses);
    }

    /**
     * ⚠️ دلیل: {@code java.util.Random} قابل پیش‌بینی است. این پروژه
     * شناسه‌ها، IVهای رمزنگاری، توکن‌ها و کدهای دعوت تولید می‌کند؛ اگر
     * هرکدام با {@code Random} ساخته شوند، مهاجم می‌تواند مقدار بعدی را
     * حدس بزند. {@code SecureRandom} تنها گزینه مجاز است.
     */
    @Test
    @DisplayName("از Random ناامن استفاده نمی‌شود")
    void noInsecureRandom() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Random")
                .because("Random قابل پیش‌بینی است؛ برای توکن و شناسه باید SecureRandom استفاده شود");

        rule.check(productionClasses);
    }

    /**
     * ⚠️ دلیل: {@code System.out} از مدیریت Log عبور نمی‌کند، پس در
     * production نه سطح دارد، نه ساختار، و نه به فایل می‌رود. بدتر،
     * ممکن است داده حساس بدون پوشاندن در آن بیفتد — دقیقاً چیزی که
     * بخش ۵۳ سند اصلی منع کرده است.
     */
    @Test
    @DisplayName("چاپ مستقیم روی خروجی استاندارد وجود ندارد")
    void noDirectConsoleOutput() {
        ArchRule standardOut = noClasses()
                .should().accessField(System.class, "out")
                .because("خروجی باید از SLF4J بگذرد تا سطح‌بندی و پوشاندن داده اعمال شود");

        ArchRule standardErr = noClasses()
                .should().accessField(System.class, "err")
                .because("خروجی باید از SLF4J بگذرد تا سطح‌بندی و پوشاندن داده اعمال شود");

        standardOut.check(productionClasses);
        standardErr.check(productionClasses);
    }

    // ─────────────────────── مرز لایه‌ها ───────────────────────

    /**
     * ⚠️ دلیل: اگر موجودیت JPA به لایه وب برسد، سریال‌سازی JSON
     * می‌تواند فیلدهای حساس (کلید رمزشده، هش توکن) را ناخواسته بیرون
     * بدهد و تغییر Schema دیتابیس به تغییر قرارداد API تبدیل می‌شود.
     * خروجی وب همیشه باید DTO باشد.
     */
    @Test
    @DisplayName("موجودیت‌های JPA وارد لایه وب نمی‌شوند")
    void entitiesDoNotReachWebLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .because("لایه وب باید DTO رد و بدل کند، نه موجودیت JPA");

        rule.check(productionClasses);
    }

    /** موجودیت‌ها جای دیگری جز لایه دامنه ندارند. */
    @Test
    @DisplayName("موجودیت‌ها در لایه وب تعریف نمی‌شوند")
    void entitiesAreNotDefinedInWebLayer() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..web..")
                .because("موجودیت جایگاهش ماژول دامنه است، نه لایه وب");

        rule.check(productionClasses);
    }

    /**
     * ⚠️ دلیل: همه موجودیت‌ها باید {@code id}، {@code version} و
     * ستون‌های حسابرسی را از {@link BaseEntity} بگیرند. اگر موجودیتی
     * جداگانه تعریف شود، قفل خوش‌بینانه یا ثبت «چه کسی» را بی‌صدا از
     * دست می‌دهد و کسی متوجه نمی‌شود تا اولین تعارض داده.
     *
     * <p>استثنای عمدی: {@code AuditLog}. جدول {@code audit_logs} فقط
     * افزودنی است (تریگر دیتابیس اجازه تغییر نمی‌دهد) و ستون
     * {@code version} و {@code updated_at} ندارد، پس نمی‌تواند از
     * {@code BaseEntity} ارث‌بری کند. این استثنا اینجا <b>صریح</b>
     * نوشته شده تا اگر روزی کسی موجودیت دیگری از این قاعده مستثنا کرد،
     * مجبور شود نامش را به همین لیست اضافه کند و دلیلش را بنویسد.
     */
    @Test
    @DisplayName("همه موجودیت‌ها از BaseEntity ارث می‌برند")
    void entitiesExtendBaseEntity() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .and().doNotHaveSimpleName("AuditLog")
                // کلاس‌های MappedSuperclass خودشان موجودیت نیستند ولی
                // annotation ندارند، پس در فیلتر نمی‌آیند.
                .should().beAssignableTo(BaseEntity.class)
                .because("id، version و ستون‌های حسابرسی باید مشترک و یکدست باشند");

        rule.check(productionClasses);
    }

    // ─────────────────────── ساختار ماژول‌ها ───────────────────────

    /**
     * مخزن‌های Spring Data باید interface باشند.
     *
     * <p>دلیل: اگر کسی مخزنی را کلاس concrete بنویسد، Spring Data آن
     * را به‌عنوان Repository تشخیص نمی‌دهد و کوئری‌های مشتق‌شده کار
     * نمی‌کنند — خطایی که در زمان اجرا و در اولین فراخوانی ظاهر
     * می‌شود، نه در بیلد.
     */
    @Test
    @DisplayName("مخزن‌ها interface هستند")
    void repositoriesAreInterfaces() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().beInterfaces()
                .because("Spring Data فقط interface‌ها را به‌عنوان Repository می‌شناسد");

        rule.check(productionClasses);
    }

    /**
     * ⚠️ دلیل: {@code @ConfigurationProperties} باید record باشد تا
     * تغییرناپذیر بماند و نتوان در زمان اجرا مقادیر امنیتی (کلید،
     * آدرس) را عوض کرد.
     */
    @Test
    @DisplayName("کلاس‌های پیکربندی record هستند")
    void configurationPropertiesAreRecords() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(ConfigurationProperties.class)
                .should().beRecords()
                .because("پیکربندی باید تغییرناپذیر باشد تا مقادیر امنیتی در زمان اجرا عوض نشوند");

        rule.check(productionClasses);
    }

    /** بسته‌های اصلی ماژول‌ها واقعاً وجود دارند و خالی نیستند. */
    @Test
    @DisplayName("بسته‌های اصلی ماژول‌ها وجود دارند")
    void expectedPackagesExist() {
        assertThat(productionClasses)
                .as("کد production باید وارد شده باشد")
                .isNotEmpty();

        for (String modulePackage : new String[]{
                "ir.omid.smartteacher.common",
                "ir.omid.smartteacher.tenant",
                "ir.omid.smartteacher.user",
                "ir.omid.smartteacher.security",
                "ir.omid.smartteacher.audit",
                "ir.omid.smartteacher.eitaa",
                "ir.omid.smartteacher.classroom",
                "ir.omid.smartteacher.student",
                "ir.omid.smartteacher.admin",
                "ir.omid.smartteacher.ai",
                "ir.omid.smartteacher.file",
                "ir.omid.smartteacher.book",
                "ir.omid.smartteacher.assignment",
                "ir.omid.smartteacher.exam",
                "ir.omid.smartteacher.billing"}) {

            assertThat(productionClasses.stream()
                    .filter(javaClass -> javaClass.getPackageName().startsWith(modulePackage)))
                    .as("بسته %s باید کلاس داشته باشد", modulePackage)
                    .isNotEmpty();
        }
    }
}
