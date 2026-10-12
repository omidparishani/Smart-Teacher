package ir.omid.smartteacher.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.TenantNotResolvedException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * آزمون زمینه مستأجر — بدون کانتکست Spring، پس سریع و قطعی.
 *
 * <p>مهم‌ترین چیزی که اینجا آزموده می‌شود «نبودِ نشتی» است: ویژگی‌ای که
 * {@code ScopedValue} را جای {@code ThreadLocal} نشاندیم. اگر زمینه پس
 * از پایان بلوک باقی بماند، درخواست بعدی روی همان Thread داده مستأجر
 * قبلی را می‌بیند — یک نشت بین‌مستأجری.
 */
class TenantContextTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-7aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-7111-8111-111111111111");

    private static final TenantPrincipal TENANT_PRINCIPAL = TenantPrincipal.ofTenantUser(
            USER_ID, TENANT_ID, null, Set.of("TEACHER"), MemberRole.TEACHER, "fa");

    // ─────────────────────── بیرون از هر محدوده ───────────────────────

    @Nested
    @DisplayName("بیرون از محدوده")
    class OutsideAnyScope {

        @Test
        @DisplayName("هیچ هویتی وجود ندارد")
        void hasNoPrincipal() {
            assertThat(TenantContext.principal()).isEmpty();
        }

        @Test
        @DisplayName("شناسه مستأجر و کاربر خالی است")
        void hasNoTenantOrUser() {
            assertThat(TenantContext.currentTenantId()).isEmpty();
            assertThat(TenantContext.currentUserId()).isEmpty();
        }

        @Test
        @DisplayName("خواندن اجباری مستأجر استثنا می‌دهد — Fail-Closed")
        void requireTenantIdThrows() {
            assertThatThrownBy(TenantContext::requireTenantId)
                    .isInstanceOf(TenantNotResolvedException.class);
        }

        @Test
        @DisplayName("خواندن اجباری هویت استثنا می‌دهد")
        void requirePrincipalThrows() {
            assertThatThrownBy(TenantContext::requirePrincipal)
                    .isInstanceOf(TenantNotResolvedException.class);
        }

        @Test
        @DisplayName("مقدار Hibernate شناسه صفر است، نه null")
        void hibernateValueIsSentinel() {
            // چرا مهم است: CurrentTenantIdentifierResolver در بازکردن
            // Session صدا زده می‌شود، حتی برای مسیر ورود که هنوز
            // مستأجری نیست. null برگرداندن، ورود را می‌شکند.
            assertThat(TenantContext.effectiveTenantIdForHibernate())
                    .isEqualTo(TenantContext.PLATFORM_SENTINEL);
        }
    }

    // ─────────────────────── داخل محدوده کاربر ناشناس ───────────────────────

    @Nested
    @DisplayName("دامنه کاربر ناشناس")
    class AnonymousScope {

        @Test
        @DisplayName("هویت ناشناس هست ولی مستأجر نیست")
        void isAnonymousWithoutTenant() {
            TenantContext.runAs(TenantPrincipal.anonymous(), () -> {
                assertThat(TenantContext.principal()).isPresent();
                assertThat(TenantContext.principal().orElseThrow().isAuthenticated()).isFalse();
                assertThat(TenantContext.currentTenantId()).isEmpty();
                assertThat(TenantContext.isPlatformScope()).isTrue();
            });
        }

        @Test
        @DisplayName("خواندن اجباری مستأجر حتی برای کاربر ناشناس هم استثنا می‌دهد")
        void requireTenantIdStillThrows() {
            TenantContext.runAs(TenantPrincipal.anonymous(), () ->
                    assertThatThrownBy(TenantContext::requireTenantId)
                            .isInstanceOf(TenantNotResolvedException.class));
        }
    }

    // ─────────────────────── داخل محدوده مستأجر ───────────────────────

    @Nested
    @DisplayName("دامنه مستأجر")
    class TenantScope {

        @Test
        @DisplayName("شناسه‌ها و هویت در دسترس‌اند")
        void exposesIdentity() {
            TenantContext.runAs(TENANT_PRINCIPAL, () -> {
                assertThat(TenantContext.requireTenantId()).isEqualTo(TENANT_ID);
                assertThat(TenantContext.requireUserId()).isEqualTo(USER_ID);
                assertThat(TenantContext.requirePrincipal().tenantRole())
                        .isEqualTo(MemberRole.TEACHER);
                assertThat(TenantContext.isPlatformScope()).isFalse();
                assertThat(TenantContext.effectiveTenantIdForHibernate()).isEqualTo(TENANT_ID);
            });
        }

        @Test
        @DisplayName("callAs مقدار را برمی‌گرداند")
        void callAsReturnsValue() {
            String result = TenantContext.callAs(TENANT_PRINCIPAL,
                    () -> TenantContext.requireTenantId().toString());

            assertThat(result).isEqualTo(TENANT_ID.toString());
        }

        @Test
        @DisplayName("پس از پایان بلوک هیچ چیزی باقی نمی‌ماند — نبودِ نشتی")
        void leavesNoResidueAfterBlock() {
            TenantContext.runAs(TENANT_PRINCIPAL,
                    () -> assertThat(TenantContext.currentTenantId()).isPresent());

            // مهم‌ترین ادعای این کلاس: اگر اینجا مقدار باقی مانده باشد،
            // درخواست بعدی روی همین Thread داده مستأجر قبلی را می‌بیند.
            assertThat(TenantContext.currentTenantId()).isEmpty();
            assertThat(TenantContext.principal()).isEmpty();
        }

        @Test
        @DisplayName("استثنای درون بلوک، زمینه را هم پاک می‌کند")
        void clearsContextOnException() {
            assertThatThrownBy(() -> TenantContext.runAs(TENANT_PRINCIPAL, () -> {
                throw new IllegalStateException("خطای عمدی برای آزمون پاک‌سازی");
            })).isInstanceOf(IllegalStateException.class);

            assertThat(TenantContext.currentTenantId()).isEmpty();
        }

        @Test
        @DisplayName("محدوده تودرتو، محدوده بیرونی را موقتاً بازنویسی می‌کند و برمی‌گرداند")
        void nestedScopeRestoresOuter() {
            UUID innerTenant = UUID.fromString("22222222-2222-7222-8222-222222222222");
            TenantPrincipal innerPrincipal = TenantPrincipal.ofTenantUser(
                    USER_ID, innerTenant, null, Set.of("TEACHER"), MemberRole.TEACHER, "fa");

            TenantContext.runAs(TENANT_PRINCIPAL, () -> {
                assertThat(TenantContext.requireTenantId()).isEqualTo(TENANT_ID);

                TenantContext.runAs(innerPrincipal, () ->
                        assertThat(TenantContext.requireTenantId()).isEqualTo(innerTenant));

                assertThat(TenantContext.requireTenantId())
                        .as("پس از خروج از محدوده درونی، مستأجر بیرونی باید برگردد")
                        .isEqualTo(TENANT_ID);
            });
        }

        @Test
        @DisplayName("مقدار null پذیرفته نمی‌شود")
        void rejectsNullPrincipal() {
            assertThatThrownBy(() -> TenantContext.runAs(null, () -> { }))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> TenantContext.callAs(null, () -> "x"))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ─────────────────────── مسیر استثنای checked ───────────────────────

    @Nested
    @DisplayName("runAsChecked")
    class CheckedPath {

        @Test
        @DisplayName("استثنای checked بدون پیچیدن منتشر می‌شود")
        void propagatesCheckedExceptionUnchanged() {
            IOException original = new IOException("خطای ورودی/خروجی عمدی");

            // این مسیر برای فیلتر Servlet حیاتی است: chain.doFilter هر دو
            // ServletException و IOException پرتاب می‌کند و اگر پیچیده
            // شوند، کانتینر رفتار درستی نشان نمی‌دهد.
            assertThatThrownBy(() -> TenantContext.runAsChecked(TENANT_PRINCIPAL, () -> {
                throw original;
            }))
                    .as("همان نمونه استثنا باید منتشر شود، نه یک نمونه پیچیده‌شده")
                    .isSameAs(original);
        }

        @Test
        @DisplayName("استثنای unchecked هم دست‌نخورده منتشر می‌شود")
        void propagatesRuntimeExceptionUnchanged() {
            IllegalStateException original = new IllegalStateException("خطای عمدی");

            assertThatThrownBy(() -> TenantContext.runAsChecked(TENANT_PRINCIPAL, () -> {
                throw original;
            })).isSameAs(original);
        }

        @Test
        @DisplayName("زمینه در طول عملیات در دسترس است و بعدش پاک می‌شود")
        void bindsContextForTheOperation() throws Exception {
            AtomicReference<Optional<UUID>> seenInside = new AtomicReference<>();

            TenantContext.runAsChecked(TENANT_PRINCIPAL,
                    () -> seenInside.set(TenantContext.currentTenantId()));

            assertThat(seenInside.get()).contains(TENANT_ID);
            assertThat(TenantContext.currentTenantId()).isEmpty();
        }
    }

    // ─────────────────────── رفتار بین ریسمان‌ها ───────────────────────

    /**
     * ⚠️ این آزمون یک <b>محدودیت</b> را تثبیت می‌کند، نه یک قابلیت.
     *
     * <p>{@code ScopedValue} برخلاف {@code InheritableThreadLocal} به
     * ریسمان‌های دیگر منتقل <b>نمی‌شود</b>. پیامدش این است که
     * {@code @Async}، {@code ExecutorService} و Jobهای زمان‌بندی‌شده
     * زمینه مستأجر را از دست می‌دهند.
     *
     * <p>این رفتار عمدی و مطلوب است: اگر زمینه بی‌سروصدا منتقل می‌شد،
     * یک Job پس‌زمینه می‌توانست روی داده مستأجری کار کند که هرگز
     * درخواستش را نداده — یا بدتر، بدون هیچ زمینه‌ای کوئری بدون فیلتر
     * بزند. «از دست دادن زمینه» به خطای واضح منجر می‌شود؛ «انتقال
     * اشتباه زمینه» به نشت بی‌صدا.
     *
     * <p>اگر روزی این تست شکست خورد، یعنی رفتار JDK عوض شده و بایستی
     * همه کدهای همزمان (async/scheduled) بازبینی شوند.
     */
    @Nested
    @DisplayName("انتقال بین ریسمان‌ها")
    class CrossThreadBehaviour {

        @Test
        @DisplayName("به ریسمان معمولی منتقل نمی‌شود — Fail-Closed")
        void isNotInheritedByPlainThreads() throws InterruptedException {
            AtomicReference<Optional<UUID>> seenOnOtherThread = new AtomicReference<>();
            AtomicReference<Throwable> failureOnOtherThread = new AtomicReference<>();

            Runnable probe = () -> {
                seenOnOtherThread.set(TenantContext.currentTenantId());
                try {
                    TenantContext.requireTenantId();
                } catch (RuntimeException e) {
                    failureOnOtherThread.set(e);
                }
            };

            Thread other = new Thread(probe, "tenant-probe");
            TenantContext.runAs(TENANT_PRINCIPAL, () -> {
                other.start();
                try {
                    other.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            assertThat(seenOnOtherThread.get())
                    .as("ریسمان دیگر نباید مستأجر را ببیند")
                    .isEmpty();
            assertThat(failureOnOtherThread.get())
                    .as("خواندن اجباری مستأجر در ریسمان دیگر باید استثنا بدهد")
                    .isInstanceOf(TenantNotResolvedException.class);
        }

        @Test
        @DisplayName("به Virtual Thread هم منتقل نمی‌شود")
        void isNotInheritedByVirtualThreads() throws Exception {
            // این مهم‌ترین حالت عملی است: application.yml برای همه
            // درخواست‌ها Virtual Thread را روشن کرده است، پس هر کد
            // async/scheduled دقیقاً همین وضعیت را می‌بیند.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<Optional<UUID>> result = TenantContext.callAs(
                        TENANT_PRINCIPAL,
                        () -> executor.submit(TenantContext::currentTenantId));

                assertThat(result.get())
                        .as("Virtual Thread نباید مستأجر را ارث‌بری کند")
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("برقراری صریح زمینه روی ریسمان دیگر کار می‌کند")
        void explicitRebindingOnAnotherThreadWorks() throws Exception {
            // راه‌حل محدودیت بالا: کدی که روی ریسمان دیگر اجرا می‌شود
            // باید زمینه را صریحاً با callAs دوباره برقرار کند.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<Optional<UUID>> result = TenantContext.callAs(
                        TENANT_PRINCIPAL,
                        () -> executor.submit(() -> TenantContext.callAs(
                                TENANT_PRINCIPAL,
                                TenantContext::currentTenantId)));

                assertThat(result.get()).contains(TENANT_ID);
            }
        }
    }
}
