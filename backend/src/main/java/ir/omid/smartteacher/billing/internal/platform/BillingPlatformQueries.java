package ir.omid.smartteacher.billing.internal.platform;

import ir.omid.smartteacher.billing.internal.api.dto.AdminSubscriptionResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * کوئری‌های JDBC فرامستأجری برای ادمین/جاب‌های billing.
 */
@Component
public class BillingPlatformQueries {

    private final JdbcTemplate jdbc;

    public BillingPlatformQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminSubscriptionResponse> listSubscriptions(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        int offset = safePage * safeSize;
        List<AdminSubscriptionResponse> rows = new ArrayList<>();
        jdbc.query("""
                select s.id, s.tenant_id, s.plan_id, p.code as plan_code, p.name as plan_name,
                       s.status, s.start_date, s.end_date, s.price_paid, s.created_at
                  from subscriptions s
                  join plans p on p.id = s.plan_id
                 order by s.created_at desc
                 limit ? offset ?
                """, rs -> {
            while (rs.next()) {
                rows.add(new AdminSubscriptionResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getObject("plan_id", UUID.class),
                        rs.getString("plan_code"),
                        rs.getString("plan_name"),
                        rs.getString("status"),
                        toInstant(rs.getTimestamp("start_date")),
                        toInstant(rs.getTimestamp("end_date")),
                        rs.getBigDecimal("price_paid"),
                        toInstant(rs.getTimestamp("created_at"))));
            }
            return null;
        }, safeSize, offset);
        return rows;
    }

    public List<ExpiringSubscriptionRow> findActiveExpiringOn(LocalDate targetDayUtc) {
        List<ExpiringSubscriptionRow> rows = new ArrayList<>();
        jdbc.query("""
                select s.id, s.tenant_id, s.plan_id, p.name as plan_name, s.end_date
                  from subscriptions s
                  join plans p on p.id = s.plan_id
                 where s.status = 'ACTIVE'
                   and s.end_date is not null
                   and (s.end_date at time zone 'UTC')::date = ?::date
                 order by s.end_date
                """, rs -> {
            while (rs.next()) {
                rows.add(new ExpiringSubscriptionRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getObject("plan_id", UUID.class),
                        rs.getString("plan_name"),
                        toInstant(rs.getTimestamp("end_date"))));
            }
            return null;
        }, targetDayUtc.toString());
        return rows;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    public record ExpiringSubscriptionRow(
            UUID subscriptionId,
            UUID tenantId,
            UUID planId,
            String planName,
            Instant endDate
    ) {
    }
}
