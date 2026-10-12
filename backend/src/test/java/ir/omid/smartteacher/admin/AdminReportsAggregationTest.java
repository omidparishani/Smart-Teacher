package ir.omid.smartteacher.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.jwt.TokenScope;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * فاز ۱۱ گام ۱ — صحت ریاضی نمودار و گزارش با دادهٔ seed شده.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminReportsAggregationTest {

    private static final UUID ROLE_SUPER_ADMIN =
            UUID.fromString("10000000-0000-7000-8000-000000000001");
    private static final UUID PLAN_FREE =
            UUID.fromString("70000000-0000-7000-8000-000000000001");

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String marker;

    @BeforeEach
    void seedAdmin() {
        marker = "rpt-" + unique();
        String username = "report-admin-" + unique();
        UUID adminId = insertSuperAdmin(username, "Report-Admin-Pass-1!");
        adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);
    }

    @Test
    @DisplayName("گزارش کاربران — شمارش وضعیت با داده seed")
    void usersReport_countsByStatus() throws Exception {
        insertUser("ACTIVE");
        insertUser("ACTIVE");
        insertUser("DISABLED");
        insertUser("SUSPENDED");

        long activeBefore = countStatus("ACTIVE");
        long disabledBefore = countStatus("DISABLED");
        long suspendedBefore = countStatus("SUSPENDED");

        // seed بالا قبلاً insert شده؛ دوباره نمی‌خوانیم — فقط اختلاف را با
        // شمارش کل فعلی می‌سنجیم که حداقل این تعداد را پوشش دهد.
        mockMvc.perform(get("/api/v1/admin/reports/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(
                        (int) (activeBefore + disabledBefore + suspendedBefore
                                + countStatus("PENDING")))))
                .andExpect(jsonPath("$.byStatus.ACTIVE").value((int) activeBefore))
                .andExpect(jsonPath("$.byStatus.DISABLED").value((int) disabledBefore))
                .andExpect(jsonPath("$.byStatus.SUSPENDED").value((int) suspendedBefore));
    }

    @Test
    @DisplayName("نمودار ۷روزه — کاربران و مستأجر و اشتراک seed شده روی روز ثابت UTC")
    void charts_includeSeededDailyCounts() throws Exception {
        // 🔴 تاریخ ثابت نباشد. پنجرهٔ «7d» غلتان است: از
        // `startOfUtcDay(اکنون − ۷ روز)` تا اکنون. یک تاریخ هاردکدشده روزی
        // از این پنجره بیرون می‌افتد و آزمون را می‌شکند — دقیقاً همان چیزی
        // که در ۲۰۲۶-۱۰-۱۱ رخ داد: `2026-10-03` از پنجره بیرون رفت و این
        // آزمون بدون هیچ تغییر در کدِ محصول شکست.
        //
        // `−۲ روز` عمدی است: همیشه داخل پنجره است و از مرز نیمه‌شب UTC هم
        // فاصله دارد، پس وابستگی به timezone JVM/کانتینر برنمی‌گردد.
        LocalDate seedDate = LocalDate.now(ZoneOffset.UTC).minusDays(2);
        String seedDay = seedDate.toString();
        Instant seedInstant = seedDate.atTime(12, 0).toInstant(ZoneOffset.UTC);
        Instant older = seedInstant.minus(2, ChronoUnit.DAYS);

        UUID userSeed = insertUserAt("ACTIVE", seedInstant);
        insertUserAt("ACTIVE", older);
        UUID tenantSeed = insertTenantAt(userSeed, seedInstant);
        insertSubscription(tenantSeed, seedInstant);
        insertSubscription(tenantSeed, older);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/charts")
                        .param("period", "7d")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("7d"))
                .andExpect(jsonPath("$.newUsers").isArray())
                .andExpect(jsonPath("$.newTenants").isArray())
                .andExpect(jsonPath("$.subscriptions").isArray())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(sumSeriesForDate(json, "newUsers", seedDay)).isGreaterThanOrEqualTo(1);
        assertThat(sumSeriesForDate(json, "newTenants", seedDay)).isGreaterThanOrEqualTo(1);
        assertThat(sumSeriesForDate(json, "subscriptions", seedDay)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("گزارش اشتراک / AI / درآمد — جمع درست")
    void subscriptionAiRevenue_math() throws Exception {
        UUID owner = insertUser("ACTIVE");
        UUID tenant = insertTenantAt(owner, Instant.now());
        insertSubscription(tenant, Instant.now());
        insertSubscription(tenant, Instant.now().minus(1, ChronoUnit.DAYS));

        insertAiUsage(tenant, 10, 20, "1.5");
        insertAiUsage(tenant, 5, 7, "0.5");

        insertInvoice(tenant, "PAID", "100.0000");
        insertInvoice(tenant, "PAID", "50.5000");
        insertInvoice(tenant, "DRAFT", "999.0000");

        long subTotal = jdbc.queryForObject("select count(*) from subscriptions", Long.class);
        mockMvc.perform(get("/api/v1/admin/reports/subscriptions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value((int) subTotal));

        mockMvc.perform(get("/api/v1/admin/reports/ai")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptTokens").value(org.hamcrest.Matchers.greaterThanOrEqualTo(15)))
                .andExpect(jsonPath("$.completionTokens")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(27)));

        BigDecimal paidSum = jdbc.queryForObject(
                "select coalesce(sum(total),0) from invoices where status='PAID'",
                BigDecimal.class);
        long paidCount = jdbc.queryForObject(
                "select count(*) from invoices where status='PAID'", Long.class);

        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidInvoiceCount").value((int) paidCount))
                .andExpect(jsonPath("$.paidTotal").value(paidSum.doubleValue()));
    }

    @Test
    @DisplayName("CSV گزارش کاربران شامل TOTAL و وضعیت‌هاست")
    void usersReportCsv_containsStatusRows() throws Exception {
        insertUser("PENDING");

        MvcResult result = mockMvc.perform(get("/api/v1/admin/reports/users.csv")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv).startsWith("status,count");
        assertThat(csv).contains("TOTAL,");
        assertThat(csv).contains("ACTIVE,");
    }

    private long countStatus(String status) {
        Long count = jdbc.queryForObject(
                "select count(*) from users where deleted_at is null and status = ?",
                Long.class, status);
        return count == null ? 0L : count;
    }

    private static long sumSeriesForDate(String json, String series, String date) {
        // شمارش ساده بدون وابستگی به ترتیب فیلدهای Jackson
        int seriesIdx = json.indexOf("\"" + series + "\"");
        if (seriesIdx < 0) {
            return -1;
        }
        int arrStart = json.indexOf('[', seriesIdx);
        int arrEnd = json.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) {
            return -1;
        }
        String arr = json.substring(arrStart, arrEnd + 1);
        int dateIdx = arr.indexOf("\"" + date + "\"");
        if (dateIdx < 0) {
            return 0;
        }
        // نزدیک‌ترین "count": بعد یا قبل از date در همان آبجکت
        int objStart = arr.lastIndexOf('{', dateIdx);
        int objEnd = arr.indexOf('}', dateIdx);
        if (objStart < 0 || objEnd < 0) {
            return 0;
        }
        String obj = arr.substring(objStart, objEnd + 1);
        int countKey = obj.indexOf("\"count\"");
        if (countKey < 0) {
            return 0;
        }
        int colon = obj.indexOf(':', countKey);
        int numStart = colon + 1;
        while (numStart < obj.length() && !Character.isDigit(obj.charAt(numStart))) {
            numStart++;
        }
        int numEnd = numStart;
        while (numEnd < obj.length() && Character.isDigit(obj.charAt(numEnd))) {
            numEnd++;
        }
        if (numStart >= numEnd) {
            return 0;
        }
        return Long.parseLong(obj.substring(numStart, numEnd));
    }

    private UUID insertSuperAdmin(String username, String rawPassword) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, passwordEncoder.encode(rawPassword), "ادمین گزارش " + unique());
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);
        return userId;
    }

    private UUID insertUser(String status) {
        return insertUserAt(status, Instant.now());
    }

    private UUID insertUserAt(String status, Instant createdAt) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, full_name, status, created_at, updated_at)
                values (?, ?, ?, ?::timestamptz, ?::timestamptz)
                """,
                userId, "کاربر " + marker + "-" + unique(), status,
                createdAt.toString(), createdAt.toString());
        return userId;
    }

    private UUID insertTenantAt(UUID ownerId, Instant createdAt) {
        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id, created_at, updated_at)
                values (?, 'TEACHER', ?, ?, ?, ?::timestamptz, ?::timestamptz)
                """,
                tenantId, "مستأجر " + marker + "-" + unique(), "t-" + unique(),
                ownerId, createdAt.toString(), createdAt.toString());
        return tenantId;
    }

    private void insertSubscription(UUID tenantId, Instant createdAt) {
        jdbc.update("""
                insert into subscriptions (id, tenant_id, plan_id, status, created_at, updated_at)
                values (?, ?, ?, 'ACTIVE', ?::timestamptz, ?::timestamptz)
                """,
                UuidV7Generator.generate(), tenantId, PLAN_FREE,
                createdAt.toString(), createdAt.toString());
    }

    private void insertAiUsage(UUID tenantId, long prompt, long completion, String cost) {
        jdbc.update("""
                insert into ai_usage_records
                  (id, tenant_id, feature, model, prompt_tokens, completion_tokens, cost)
                values (?, ?, 'CHAT', 'mock-model', ?, ?, ?::numeric)
                """,
                UuidV7Generator.generate(), tenantId, prompt, completion, cost);
    }

    private void insertInvoice(UUID tenantId, String status, String total) {
        String number = "INV-RPT-" + unique() + "-" + System.nanoTime();
        jdbc.update("""
                insert into invoices
                  (id, tenant_id, invoice_number, subtotal, discount, tax, total, currency, status)
                values (?, ?, ?, ?::numeric, 0, 0, ?::numeric, 'IRT', ?)
                """,
                UuidV7Generator.generate(), tenantId, number, total, total, status);
    }

    private static String unique() {
        return Integer.toString(SEQ.incrementAndGet());
    }
}
