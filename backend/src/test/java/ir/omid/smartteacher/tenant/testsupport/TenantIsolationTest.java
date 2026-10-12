package ir.omid.smartteacher.tenant.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.TenantNotResolvedException;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * آزمون جداسازی مستأجر — مهم‌ترین قید امنیتی فاز ۱.
 *
 * <p>هدف: اثبات اینکه فیلتر خودکار {@code @TenantId} واقعاً کار می‌کند،
 * نه اینکه فقط درست پیکربندی شده به نظر برسد. این تفاوت مهم است: یک
 * اشتباه رایج، پیکربندی‌ای است که بی‌صدا هیچ فیلتری اعمال نمی‌کند و
 * تا اولین نشت داده کسی متوجه نمی‌شود.
 *
 * <p>روش: یک موجودیت کاوشگر با {@code @TenantId} در دو مستأجر مختلف
 * ذخیره می‌شود و سپس دیده می‌شود که هر مستأجر فقط داده خودش را می‌بیند.
 *
 * <p><b>⚠️ این تست معنادار نیست مگر اینکه واقعاً روی PostgreSQL اجرا
 * شود.</b> با H2 نتایج متفاوت و گمراه‌کننده می‌شود. به همین دلیل
 * Testcontainers اجباری است و هیچ پروفایل in-memory در پروژه نیست.
 *
 * <p>توجه: جدول {@code isolation_probe} توسط Migration فقط-تست
 * {@code db/test-migration/V900__isolation_probe.sql} ساخته می‌شود.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TenantIsolationTest {

    /**
     * تصویر PostgreSQL برای این تست.
     *
     * <p>پیش‌فرض عمداً همان نسخه Production است — PostgreSQL 17. تستِ
     * جداسازی مستأجر باید روی همان نسخه‌ای اجرا شود که در Production
     * کار می‌کند، وگرنه «سبز بودنش» درباره سیستم واقعی چیزی نمی‌گوید.
     *
     * <p>اگر محیطی به Docker Hub دسترسی نداشت، می‌توان تصویری که از قبل
     * روی ماشین هست را داد:
     * <pre>{@code
     * mvn test -Dtest=TenantIsolationTest -Dtest.postgres.image=postgres:17-alpine
     * }</pre>
     *
     * <p><b>⚠️ این یک تخفیف است، نه معادل.</b> اگر از این راه استفاده
     * شود، نتیجه فقط تا حدی معتبر است که نسخه جایگزین از نظر قابلیت‌های
     * به‌کاررفته — {@code jsonb}، {@code pgcrypto}، {@code pg_trgm} و
     * فیلتر {@code @TenantId} هایبرنیت — با ۱۷ یکی باشد. هر اجرای سبزی
     * که با نسخه‌ای جز ۱۷ گرفته شده، باید در
     * {@code docs/verification-log.md} با همان نسخه ثبت شود.
     */
    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    // ── شناسه‌های ثابت: خواندن خروجی شکست را آسان می‌کند ──
    // رقم نسخه (7) و variant (8) رعایت شده تا اینها UUID معتبر باشند.
    private static final UUID TENANT_A = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-7aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-7bbb-8bbb-bbbbbbbbbbbb");

    private static final TenantPrincipal PRINCIPAL_A = TenantPrincipal.ofTenantUser(
            USER_A, TENANT_A, null, Set.of("TEACHER"), MemberRole.TEACHER, "fa");

    private static final TenantPrincipal PRINCIPAL_B = TenantPrincipal.ofTenantUser(
            USER_B, TENANT_B, null, Set.of("TEACHER"), MemberRole.TEACHER, "fa");

    @Autowired
    private IsolationProbeRepository probes;

    /**
     * برچسب یکتا برای هر تست.
     *
     * <p>لازم است چون رکوردها بین تست‌ها باقی می‌مانند (هیچ rollback‌ای
     * وجود ندارد — نمی‌توانیم از {@code @Transactional} استفاده کنیم،
     * چون آن یک Session واحد می‌سازد و آن‌وقت شناسه مستأجر فقط یک بار
     * هنگام بازشدن Session خوانده می‌شود و تست بی‌معنا می‌گردد).
     */
    private static String newLabel() {
        return "probe-" + UUID.randomUUID();
    }

    // ─────────────────────────── آزمون‌ها ───────────────────────────

    @Test
    @DisplayName("رکورد ذخیره‌شده مُهر مستأجر جاری را می‌خورد")
    void savedEntityIsStampedWithCurrentTenant() {
        String label = newLabel();

        TenantContext.runAs(PRINCIPAL_A, () -> probes.save(new IsolationProbe(label)));

        // کوئری بومی عمداً فیلتر مستأجر را دور می‌زند تا مقدار واقعی
        // ستون دیده شود — نه آنچه فیلتر نشان می‌دهد.
        List<UUID> rawTenantIds = probes.findRawTenantIdsByLabel(label);

        assertThat(rawTenantIds)
                .as("ستون tenant_id باید مقدار مستأجر جاری را داشته باشد")
                .containsExactly(TENANT_A);
    }

    @Test
    @DisplayName("هر مستأجر فقط رکوردهای خودش را می‌شمارد")
    void countIsScopedToCurrentTenant() {
        String label = newLabel();

        TenantContext.runAs(PRINCIPAL_A, () -> probes.save(new IsolationProbe(label)));

        long seenByA = TenantContext.callAs(PRINCIPAL_A, () -> probes.countByLabel(label));
        long seenByB = TenantContext.callAs(PRINCIPAL_B, () -> probes.countByLabel(label));

        assertThat(seenByA).as("مستأجر الف باید رکورد خودش را ببیند").isEqualTo(1);
        assertThat(seenByB).as("مستأجر ب نباید هیچ رکوردی ببیند").isZero();
    }

    @Test
    @DisplayName("findById از مستأجر دیگر رکورد را برنمی‌گرداند")
    void findByIdFromAnotherTenantReturnsEmpty() {
        String label = newLabel();

        UUID id = TenantContext.callAs(PRINCIPAL_A,
                () -> probes.save(new IsolationProbe(label)).getId());

        assertThat(TenantContext.callAs(PRINCIPAL_A, () -> probes.findById(id)))
                .as("مالک باید رکورد را ببیند")
                .isPresent();

        assertThat(TenantContext.callAs(PRINCIPAL_B, () -> probes.findById(id)))
                .as("مستأجر دیگر باید همان شناسه را ناموجود ببیند — نه خطای ۴۰۳")
                .isEmpty();
    }

    @Test
    @DisplayName("شمارش کل در هر مستأجر فقط داده خودش را شامل می‌شود")
    void totalCountDoesNotLeakAcrossTenants() {
        String labelA = newLabel();
        String labelB = newLabel();

        // ⚠️ روی «تفاضل» سنجیده می‌شود، نه مقدار مطلق. رکوردها بین
        // تست‌ها باقی می‌مانند، پس عدد مطلق به ترتیب اجرای تست‌ها وابسته
        // است و تست را شکننده می‌کند. آنچه واقعاً معنا دارد این است:
        // افزودن یک رکورد به الف باید شمارش الف را دقیقاً یکی زیاد کند
        // و شمارش ب را اصلاً تکان ندهد. اگر رکورد الف به ب نشت کند،
        // تفاضل ب می‌شود ۲ و تست می‌افتد.
        long beforeA = TenantContext.callAs(PRINCIPAL_A, probes::count);
        long beforeB = TenantContext.callAs(PRINCIPAL_B, probes::count);

        TenantContext.runAs(PRINCIPAL_A, () -> probes.save(new IsolationProbe(labelA)));
        TenantContext.runAs(PRINCIPAL_B, () -> probes.save(new IsolationProbe(labelB)));

        long afterA = TenantContext.callAs(PRINCIPAL_A, probes::count);
        long afterB = TenantContext.callAs(PRINCIPAL_B, probes::count);

        assertThat(afterA - beforeA)
                .as("نوشتن در الف باید شمارش الف را دقیقاً یکی زیاد کند")
                .isEqualTo(1);
        assertThat(afterB - beforeB)
                .as("نوشتن در ب باید شمارش ب را دقیقاً یکی زیاد کند — نه دو تا")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("دامنه پلتفرم هیچ داده مستأجری نمی‌بیند — Fail-Closed")
    void platformScopeSeesNoTenantData() {
        String label = newLabel();

        TenantContext.runAs(PRINCIPAL_A, () -> probes.save(new IsolationProbe(label)));

        // دامنه پلتفرم با شناسه صفر (PLATFORM_SENTINEL) به Hibernate
        // معرفی می‌شود. این «دسترسی به همه مستأجرها» نیست: فیلتر روی
        // tenant_id = صفر می‌ماند که به هیچ مستأجری تعلق ندارد.
        //
        // ⚠️ سنجش روی همین رکورد است، نه شمارش کل. دلیلش ترتیب اجرای
        // تست‌ها است: این کلاس رکورد بین تست‌ها را پاک نمی‌کند، پس
        // شمارش کل به اینکه کدام تست زودتر اجرا شده وابسته می‌شد و
        // تست شکننده می‌گشت. کوئری برچسب‌محور آن وابستگی را حذف می‌کند.
        //
        // (این کلاس تست دیگری هم دارد که در دامنه پلتفرم می‌نویسد.
        // اندازه‌گیری نشان داد آن نوشتن با TenantNotResolvedException
        // رد می‌شود و رکوردی با tenant_id = صفر ساخته نمی‌شود؛ پس در
        // عمل رکوردی از آن مسیر به اینجا نمی‌رسد. با این حال
        // برچسب‌محور ماندن سنجش، درست‌تر است — به رفتار تست دیگر
        // وابسته نمی‌شود.)
        long seenByPlatform = TenantContext.callAs(TenantPrincipal.platform(),
                () -> probes.countByLabel(label));

        assertThat(seenByPlatform)
                .as("دامنه پلتفرم نباید داده مستأجر را ببیند")
                .isZero();

        assertThat(TenantContext.callAs(PRINCIPAL_A, () -> probes.countByLabel(label)))
                .as("همان رکورد باید برای صاحبش دیده شود — وگرنه صفرِ بالا بی‌معنا است")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("دامنه پلتفرم هم رکورد تازه را به هیچ مستأجری نسبت نمی‌دهد")
    void platformScopeWriteDoesNotLandInATenant() {
        String label = newLabel();

        // ⚠️ این تست یک بار با فرض اشتباه نوشته شده بود. فرض این بود که
        // نوشتن موفق می‌شود و رکورد «مُهر صفر» می‌خورد، چون resolver
        // هنگام باز کردن Session در دامنه پلتفرم PLATFORM_SENTINEL
        // برمی‌گرداند. اندازه‌گیری واقعی خلافش را نشان داد: Hibernate
        // هنگام persist ستون @TenantId را از resolver پر نمی‌کند، پس
        // @PrePersist در TenantAwareEntity مقدار null می‌بیند و
        // requireTenantId پرتاب می‌کند. یعنی Fail-Closed واقعاً رخ
        // می‌دهد و رکوردی ساخته نمی‌شود.
        // (این با یک Probe موقت که اگر استثنا پرتاب نمی‌شد تست را
        // می‌شکست تعیین شد — نه با حدس.)
        assertThatThrownBy(() -> TenantContext.runAs(TenantPrincipal.platform(),
                () -> probes.save(new IsolationProbe(label))))
                .as("نوشتن بدون مستأجر باید رد شود، نه بی‌صدا موفق")
                .isInstanceOf(TenantNotResolvedException.class);

        // و مهم‌تر از پرتاب استثنا: هیچ رکوردی در جدول نمانده باشد.
        // کوئری خام است چون کوئری معمولی فیلتر مستأجر دارد و ممکن است
        // رکورد نشت‌کرده را پنهان کند.
        assertThat(probes.findRawTenantIdsByLabel(label))
                .as("پس از رد شدن ذخیره، نباید هیچ رکوردی در جدول ساخته شده باشد")
                .isEmpty();

        assertThat(TenantContext.callAs(PRINCIPAL_A, () -> probes.countByLabel(label)))
                .as("نوشتن در دامنه پلتفرم نباید در مستأجر الف دیده شود")
                .isZero();
        assertThat(TenantContext.callAs(PRINCIPAL_B, () -> probes.countByLabel(label)))
                .as("نوشتن در دامنه پلتفرم نباید در مستأجر ب دیده شود")
                .isZero();
    }
}
