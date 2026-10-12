package ir.omid.smartteacher.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.billing.internal.api.dto.CheckoutRequest;
import ir.omid.smartteacher.billing.internal.api.dto.PlanResponse;
import ir.omid.smartteacher.billing.internal.api.dto.SubscriptionResponse;
import ir.omid.smartteacher.billing.internal.application.BillingService;
import ir.omid.smartteacher.billing.internal.application.SubscriptionExpiryJob;
import ir.omid.smartteacher.billing.UsageMetering;
import ir.omid.smartteacher.billing.internal.application.UsageMeteringService;
import ir.omid.smartteacher.billing.internal.domain.Coupon;
import ir.omid.smartteacher.billing.internal.domain.CouponRepository;
import ir.omid.smartteacher.billing.internal.domain.Plan;
import ir.omid.smartteacher.billing.internal.domain.PlanRepository;
import ir.omid.smartteacher.billing.internal.domain.Subscription;
import ir.omid.smartteacher.billing.internal.domain.SubscriptionRepository;
import ir.omid.smartteacher.billing.internal.payment.MockPaymentGateway;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * برش عمودی فاز ۱۰ — Mock پرداخت، کوپن، سقف مصرف، callback.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BillingVerticalSliceTest {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BillingService billingService;

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UsageMeteringService usageMeteringService;

    @Autowired
    private SubscriptionExpiryJob expiryJob;

    @Autowired
    private MockPaymentGateway mockPaymentGateway;

    @Autowired
    private PaymentGateway paymentGateway;

    @Test
    @DisplayName("MockPaymentGateway در پروفایل تست فعال است")
    void mockGatewayAvailable() {
        assertThat(paymentGateway.code()).isEqualTo("MOCK");
        assertThat(mockPaymentGateway.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("فهرست پلن‌های عمومی بدون احراز هویت")
    void listPublicPlans() {
        List<PlanResponse> plans = billingService.listPublicPlans();
        assertThat(plans).isNotEmpty();
        assertThat(plans).anyMatch(p -> "FREE".equals(p.code()));
        assertThat(plans).anyMatch(p -> "PRO".equals(p.code()));
    }

    @Test
    @DisplayName("checkout با Mock اشتراک را فعال می‌کند")
    void mockPayment_success_activatesSubscription() {
        Actor teacher = newTenant();
        Plan pro = planRepository.findByCode("PRO").orElseThrow();

        SubscriptionResponse result = TenantContext.callAs(teacher.principal(), () ->
                billingService.checkout(new CheckoutRequest(pro.getId())));

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.planCode()).isEqualTo("PRO");
        assertThat(result.invoiceNumber()).startsWith("INV-");
        assertThat(result.paymentAuthority()).startsWith("mock-");

        SubscriptionResponse current = TenantContext.callAs(teacher.principal(),
                billingService::currentSubscription);
        assertThat(current).isNotNull();
        assertThat(current.id()).isEqualTo(result.id());

        TenantContext.callAs(teacher.principal(), () -> {
            entitlementService.requireFeature("AI_CHAT");
            return null;
        });
    }

    @Test
    @DisplayName("expiredSubscription_blocksPremiumFeature")
    void expiredSubscription_blocksPremiumFeature() {
        Actor teacher = newTenant();
        Plan pro = planRepository.findByCode("PRO").orElseThrow();

        TenantContext.callAs(teacher.principal(), () -> {
            Subscription expired = new Subscription(pro.getId());
            expired.setStatus("ACTIVE");
            expired.setStartDate(Instant.now().minus(60, ChronoUnit.DAYS));
            expired.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));
            expired.setPricePaid(pro.getPrice());
            subscriptionRepository.save(expired);
            return null;
        });

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () -> {
            entitlementService.requireFeature("AI_CHAT");
            return null;
        })).isInstanceOf(FeatureNotEntitledException.class);

        assertThat(TenantContext.callAs(teacher.principal(),
                () -> entitlementService.isEnabled("AI_CHAT"))).isFalse();
    }

    @Test
    @DisplayName("expiry job اشتراک گذشته را EXPIRED می‌کند و Feature را مسدود می‌کند")
    void expiryJob_blocksFeature() {
        Actor teacher = newTenant();
        Plan pro = planRepository.findByCode("PRO").orElseThrow();

        TenantContext.callAs(teacher.principal(), () -> {
            Subscription sub = new Subscription(pro.getId());
            sub.setStatus("ACTIVE");
            sub.setStartDate(Instant.now().minus(40, ChronoUnit.DAYS));
            sub.setEndDate(Instant.now().minus(1, ChronoUnit.HOURS));
            sub.setPricePaid(pro.getPrice());
            subscriptionRepository.save(sub);
            return null;
        });

        expiryJob.expireSubscriptions();

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () -> {
            entitlementService.requireFeature("AI_CHAT");
            return null;
        })).isInstanceOf(FeatureNotEntitledException.class);
    }

    @Test
    @DisplayName("callback idempotent — تکرار همان authority اشتراک دوم نمی‌سازد")
    void callback_idempotent() {
        Actor teacher = newTenant();
        Plan pro = planRepository.findByCode("PRO").orElseThrow();

        SubscriptionResponse first = TenantContext.callAs(teacher.principal(), () ->
                billingService.checkout(new CheckoutRequest(pro.getId())));
        String authority = first.paymentAuthority();

        SubscriptionResponse second = TenantContext.callAs(teacher.principal(), () ->
                billingService.processPaymentCallback(authority));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.status()).isEqualTo("ACTIVE");

        Long count = jdbc.queryForObject(
                "select count(*) from subscriptions where tenant_id = ?",
                Long.class, teacher.tenantId());
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("usageLimit_isEnforced")
    void usageLimit_isEnforced() {
        Actor teacher = newTenant();
        Plan free = planRepository.findByCode("FREE").orElseThrow();

        TenantContext.callAs(teacher.principal(), () -> {
            billingService.checkout(new CheckoutRequest(free.getId()));
            return null;
        });

        // FREE: maxAIRequests = 50 — ۵۱‌ام باید رد شود
        TenantContext.callAs(teacher.principal(), () -> {
            for (int i = 0; i < 50; i++) {
                usageMeteringService.consume(UsageMetering.METRIC_AI_REQUESTS, 1);
            }
            assertThatThrownBy(() ->
                    usageMeteringService.consume(UsageMetering.METRIC_AI_REQUESTS, 1))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USAGE_LIMIT_REACHED);
            return null;
        });
    }

    @Test
    @DisplayName("usageLimit_isEnforcedAtomicUnderConcurrency")
    void usageLimit_isEnforcedAtomicUnderConcurrency() throws Exception {
        Actor teacher = newTenant();
        Plan free = planRepository.findByCode("FREE").orElseThrow();

        TenantContext.callAs(teacher.principal(), () -> {
            billingService.checkout(new CheckoutRequest(free.getId()));
            // ردیف شمارنده را از قبل بساز تا رقابت INSERT concurrent نباشد
            usageMeteringService.consume(UsageMetering.METRIC_AI_REQUESTS, 1);
            return null;
        });
        jdbc.update(
                "update usage_counters set used_value = 0 where metric = ? and tenant_id = ?",
                UsageMetering.METRIC_AI_REQUESTS,
                teacher.tenantId());

        int threads = 60;
        int limit = 50;
        CountDownLatch start = new CountDownLatch(1);
        // pool ≤ hikari.maximum-pool-size تا صف اتصال باعث خطای جعلی نشود
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                await(start);
                try {
                    TenantContext.callAs(teacher.principal(), () -> {
                        usageMeteringService.consume(UsageMetering.METRIC_AI_REQUESTS, 1);
                        return null;
                    });
                    successes.incrementAndGet();
                } catch (RuntimeException ex) {
                    failures.incrementAndGet();
                }
            }));
        }

        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdownNow();

        assertThat(successes.get()).isEqualTo(limit);
        assertThat(failures.get()).isEqualTo(threads - limit);

        Long used = jdbc.queryForObject(
                """
                select used_value from usage_counters
                where metric = ? and tenant_id = ?
                """,
                Long.class,
                UsageMetering.METRIC_AI_REQUESTS,
                teacher.tenantId());
        assertThat(used).isEqualTo((long) limit);
    }

    @Test
    @DisplayName("کوپن منقضی رد می‌شود")
    void expiredCoupon_blocks() {
        Actor teacher = newTenant();
        Plan pro = planRepository.findByCode("PRO").orElseThrow();

        Coupon coupon = new Coupon(
                "EXPIRED" + unique().toUpperCase(),
                "PERCENTAGE",
                BigDecimal.TEN,
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.DAYS));
        coupon.setUsageLimit(10);
        couponRepository.save(coupon);

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () ->
                billingService.checkout(new CheckoutRequest(pro.getId(), coupon.getCode()))))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_INVALID);
    }

    @Test
    @DisplayName("redeem همزمان کوپن با usage_limit=1 فقط یک‌بار موفق می‌شود")
    void coupon_concurrency_singleRedeem() throws Exception {
        Plan pro = planRepository.findByCode("PRO").orElseThrow();
        String code = "ONCE" + unique().toUpperCase();
        Coupon coupon = new Coupon(
                code, "FIXED", BigDecimal.valueOf(1000),
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(7, ChronoUnit.DAYS));
        coupon.setUsageLimit(1);
        couponRepository.save(coupon);

        Actor a = newTenant();
        Actor b = newTenant();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        Future<?> f1 = pool.submit(() -> {
            await(start);
            try {
                TenantContext.callAs(a.principal(), () ->
                        billingService.checkout(new CheckoutRequest(pro.getId(), code)));
                successes.incrementAndGet();
            } catch (RuntimeException ex) {
                failures.incrementAndGet();
            }
        });
        Future<?> f2 = pool.submit(() -> {
            await(start);
            try {
                TenantContext.callAs(b.principal(), () ->
                        billingService.checkout(new CheckoutRequest(pro.getId(), code)));
                successes.incrementAndGet();
            } catch (RuntimeException ex) {
                failures.incrementAndGet();
            }
        });

        start.countDown();
        f1.get();
        f2.get();
        pool.shutdownNow();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(1);

        Integer used = jdbc.queryForObject(
                "select used_count from coupons where code = ?", Integer.class, code);
        assertThat(used).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Actor newTenant() {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "کاربر " + unique());
        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """,
                tenantId, "مدرسه " + unique(), "school-" + unique(), userId);
        return new Actor(userId, tenantId, TenantPrincipal.ofTenantUser(
                userId, tenantId, null, Set.of(MemberRole.TEACHER.name()),
                MemberRole.TEACHER, "fa"));
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
