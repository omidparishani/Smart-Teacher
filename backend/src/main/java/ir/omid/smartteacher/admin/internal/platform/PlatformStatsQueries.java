package ir.omid.smartteacher.admin.internal.platform;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * شمارنده‌ها و تجمیع‌های فرامستأجری برای داشبورد/گزارش سوپرادمین.
 *
 * <p><b>🔴 چرا JDBC و نه Repository دامنه؟</b> جدول‌های مستأجر‌محور
 * فیلتر خودکار {@code @TenantId} دارند. کوئری JPA از زمینه پلتفرم
 * (بدون مستأجر) با sentinel صفر، همیشه خالی برمی‌گردد. مسیر مجاز
 * دور زدن فیلتر برای گزارش پلتفرم در {@code docs/multi-tenancy.md}
 * بخش ۱۰ همین بسته {@code internal.platform} است.
 *
 * <p>⚠️ نام کلاس عمداً به {@code Repository} ختم نمی‌شود: ArchUnit همه
 * {@code *Repository} را interface می‌خواهد (Spring Data).
 *
 * <p>⚠️ SQL خام فقط اینجا و فقط برای شمارش/تجمیع؛ نوشتن دامنه ممنوع است.
 */
@Component
public class PlatformStatsQueries {

    private final JdbcTemplate jdbc;

    public PlatformStatsQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** تعداد کلاس‌های حذف‌نشده در همه مستأجرها. */
    public long countClassrooms() {
        return nullToZero(jdbc.queryForObject(
                "select count(*) from classrooms where deleted_at is null",
                Long.class));
    }

    /** تعداد دانش‌آموزان حذف‌نشده در همه مستأجرها. */
    public long countStudents() {
        return nullToZero(jdbc.queryForObject(
                "select count(*) from students where deleted_at is null",
                Long.class));
    }

    /**
     * شمارش کاربران حذف‌نشده بر اساس وضعیت.
     *
     * @return نقشهٔ وضعیت → تعداد؛ وضعیت‌های غایب با ۰ پر نمی‌شوند
     */
    public Map<String, Long> countUsersByStatus() {
        return queryStatusCounts("""
                select status, count(*) as cnt
                from users
                where deleted_at is null
                group by status
                order by status
                """);
    }

    /** مجموع کاربران حذف‌نشده. */
    public long countUsersTotal() {
        return nullToZero(jdbc.queryForObject(
                "select count(*) from users where deleted_at is null",
                Long.class));
    }

    /** شمارش اشتراک‌ها بر اساس وضعیت. */
    public Map<String, Long> countSubscriptionsByStatus() {
        return queryStatusCounts("""
                select status, count(*) as cnt
                from subscriptions
                group by status
                order by status
                """);
    }

    public long countSubscriptionsTotal() {
        return nullToZero(jdbc.queryForObject(
                "select count(*) from subscriptions",
                Long.class));
    }

    /**
     * سری روزانه کاربران جدید در بازهٔ {@code [fromInclusive, toExclusive)}.
     */
    public List<DailyBucket> dailyNewUsers(Instant fromInclusive, Instant toExclusive) {
        return dailyBuckets("""
                select (created_at at time zone 'UTC')::date as day, count(*) as cnt
                from users
                where deleted_at is null
                  and created_at >= ?::timestamptz
                  and created_at < ?::timestamptz
                group by day
                order by day
                """, fromInclusive, toExclusive);
    }

    public List<DailyBucket> dailyNewTenants(Instant fromInclusive, Instant toExclusive) {
        return dailyBuckets("""
                select (created_at at time zone 'UTC')::date as day, count(*) as cnt
                from tenants
                where deleted_at is null
                  and created_at >= ?::timestamptz
                  and created_at < ?::timestamptz
                group by day
                order by day
                """, fromInclusive, toExclusive);
    }

    public List<DailyBucket> dailyNewSubscriptions(Instant fromInclusive, Instant toExclusive) {
        return dailyBuckets("""
                select (created_at at time zone 'UTC')::date as day, count(*) as cnt
                from subscriptions
                where created_at >= ?::timestamptz
                  and created_at < ?::timestamptz
                group by day
                order by day
                """, fromInclusive, toExclusive);
    }

    /** درآمد روزانه از فاکتورهای PAID. */
    public List<DailyRevenueBucket> dailyRevenue(Instant fromInclusive, Instant toExclusive) {
        List<DailyRevenueBucket> rows = new ArrayList<>();
        jdbc.query("""
                select (coalesce(issued_at, created_at) at time zone 'UTC')::date as day,
                       count(*) as cnt,
                       coalesce(sum(total), 0) as revenue
                  from invoices
                 where status = 'PAID'
                   and coalesce(issued_at, created_at) >= ?::timestamptz
                   and coalesce(issued_at, created_at) < ?::timestamptz
                 group by day
                 order by day
                """, rs -> {
            while (rs.next()) {
                Date day = rs.getDate("day");
                rows.add(new DailyRevenueBucket(
                        day.toLocalDate(),
                        rs.getLong("cnt"),
                        rs.getBigDecimal("revenue")));
            }
            return null;
        }, fromInclusive.toString(), toExclusive.toString());
        return rows;
    }

    /** برترین کاربران بر اساس هزینه AI — {@code created_by} در ai_usage_records. */
    public List<UserAiUsageBucket> aiUsageByUser(int topN) {
        int limit = Math.max(1, Math.min(topN, 100));
        List<UserAiUsageBucket> rows = new ArrayList<>();
        jdbc.query("""
                select coalesce(u.id, r.created_by) as user_id,
                       coalesce(u.full_name, 'unknown') as user_name,
                       count(*) as request_count,
                       coalesce(sum(r.prompt_tokens), 0) as prompt_tokens,
                       coalesce(sum(r.completion_tokens), 0) as completion_tokens,
                       coalesce(sum(r.cost), 0) as total_cost
                  from ai_usage_records r
                  left join users u on u.id = r.created_by
                 where r.created_by is not null
                 group by coalesce(u.id, r.created_by), coalesce(u.full_name, 'unknown')
                 order by total_cost desc, request_count desc
                 limit ?
                """, rs -> {
            while (rs.next()) {
                UUID userId = rs.getObject("user_id", UUID.class);
                rows.add(new UserAiUsageBucket(
                        userId,
                        rs.getString("user_name"),
                        rs.getLong("request_count"),
                        rs.getLong("prompt_tokens"),
                        rs.getLong("completion_tokens"),
                        rs.getBigDecimal("total_cost")));
            }
            return null;
        }, limit);
        return rows;
    }

    /** جمع مصرف AI در همه مستأجرها. */
    public AiUsageAggregate sumAiUsage() {
        return jdbc.query("""
                select count(*) as request_count,
                       coalesce(sum(prompt_tokens), 0) as prompt_tokens,
                       coalesce(sum(completion_tokens), 0) as completion_tokens,
                       coalesce(sum(cost), 0) as total_cost
                from ai_usage_records
                """, rs -> {
            rs.next();
            return new AiUsageAggregate(
                    rs.getLong("request_count"),
                    rs.getLong("prompt_tokens"),
                    rs.getLong("completion_tokens"),
                    rs.getBigDecimal("total_cost"));
        });
    }

    /** هزینه AI به تفکیک مدل. */
    public List<NamedCostBucket> aiCostByModel() {
        List<NamedCostBucket> rows = new ArrayList<>();
        jdbc.query("""
                select model as name,
                       count(*) as request_count,
                       coalesce(sum(cost), 0) as total_cost
                  from ai_usage_records
                 group by model
                 order by total_cost desc, model
                """, rs -> {
            while (rs.next()) {
                rows.add(new NamedCostBucket(
                        rs.getString("name"),
                        rs.getLong("request_count"),
                        rs.getBigDecimal("total_cost")));
            }
            return null;
        });
        return rows;
    }

    /**
     * هزینه AI به تفکیک ارائه‌دهنده — از join روی {@code ai_models}/{@code ai_providers}
     * اگر مدل شناخته نشود، {@code unknown}.
     */
    public List<NamedCostBucket> aiCostByProvider() {
        List<NamedCostBucket> rows = new ArrayList<>();
        jdbc.query("""
                select coalesce(p.code, 'unknown') as name,
                       count(*) as request_count,
                       coalesce(sum(u.cost), 0) as total_cost
                  from ai_usage_records u
                  left join ai_models m on m.code = u.model
                  left join ai_providers p on p.id = m.provider_id
                 group by coalesce(p.code, 'unknown')
                 order by total_cost desc, name
                """, rs -> {
            while (rs.next()) {
                rows.add(new NamedCostBucket(
                        rs.getString("name"),
                        rs.getLong("request_count"),
                        rs.getBigDecimal("total_cost")));
            }
            return null;
        });
        return rows;
    }

    /** تعداد اشتراک ACTIVE روی پلن FREE در برابر غیر FREE. */
    public ActivePlanSplit countActiveFreeVsPaid() {
        Long free = jdbc.queryForObject("""
                select count(*)
                  from subscriptions s
                  join plans p on p.id = s.plan_id
                 where s.status = 'ACTIVE' and p.code = 'FREE'
                """, Long.class);
        Long paid = jdbc.queryForObject("""
                select count(*)
                  from subscriptions s
                  join plans p on p.id = s.plan_id
                 where s.status = 'ACTIVE' and p.code <> 'FREE'
                """, Long.class);
        return new ActivePlanSplit(nullToZero(free), nullToZero(paid));
    }

    /** تجمیع درآمد از فاکتورها. */
    public RevenueAggregate sumRevenue() {
        Map<String, Long> byStatus = queryStatusCounts("""
                select status, count(*) as cnt
                from invoices
                group by status
                order by status
                """);

        Long paidCount = jdbc.queryForObject(
                "select count(*) from invoices where status = 'PAID'",
                Long.class);
        BigDecimal paidTotal = jdbc.queryForObject(
                "select coalesce(sum(total), 0) from invoices where status = 'PAID'",
                BigDecimal.class);
        String currency = jdbc.query("""
                select currency
                from invoices
                where status = 'PAID'
                group by currency
                order by count(*) desc
                limit 1
                """, rs -> rs.next() ? rs.getString(1) : null);

        return new RevenueAggregate(
                nullToZero(paidCount),
                paidTotal == null ? BigDecimal.ZERO : paidTotal,
                currency,
                byStatus);
    }

    private Map<String, Long> queryStatusCounts(String sql) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            while (rs.next()) {
                result.put(rs.getString("status"), rs.getLong("cnt"));
            }
            return null;
        });
        return result;
    }

    private List<DailyBucket> dailyBuckets(String sql,
                                           Instant fromInclusive,
                                           Instant toExclusive) {
        List<DailyBucket> rows = new ArrayList<>();
        jdbc.query(sql, rs -> {
            while (rs.next()) {
                Date day = rs.getDate("day");
                LocalDate local = day.toLocalDate();
                rows.add(new DailyBucket(local, rs.getLong("cnt")));
            }
            return null;
        }, fromInclusive.toString(), toExclusive.toString());
        return rows;
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /** سطل روزانه UTC. */
    public record DailyBucket(LocalDate day, long count) {
        public String dateIso() {
            return day.toString();
        }
    }

    public record AiUsageAggregate(
            long requestCount,
            long promptTokens,
            long completionTokens,
            BigDecimal totalCost
    ) {
    }

    public record RevenueAggregate(
            long paidInvoiceCount,
            BigDecimal paidTotal,
            String currency,
            Map<String, Long> byStatus
    ) {
    }

    public record NamedCostBucket(String name, long requestCount, BigDecimal totalCost) {
    }

    public record ActivePlanSplit(long freeActive, long paidActive) {
    }

    public record DailyRevenueBucket(LocalDate day, long invoiceCount, BigDecimal revenue) {
        public String dateIso() {
            return day.toString();
        }
    }

    public record UserAiUsageBucket(
            UUID userId,
            String userName,
            long requestCount,
            long promptTokens,
            long completionTokens,
            BigDecimal totalCost
    ) {
    }

    /** ابتدای روز UTC برای یک Instant. */
    public static Instant startOfUtcDay(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
