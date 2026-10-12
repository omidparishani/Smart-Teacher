package ir.omid.smartteacher.eitaa.internal.application;

import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * جاب ارسال پیام‌های زمان‌بندی‌شده سررسید.
 *
 * <p>مستأجرها با JDBC خام کشف می‌شوند (بدون فیلتر Hibernate)، سپس
 * برای هر مستأجر زمینه برقرار و ارسال با Mock/آداپتور انجام می‌شود.
 */
@Component
@ConditionalOnProperty(name = "app.eitaa.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledMessageJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageJob.class);

    private final JdbcTemplate jdbc;
    private final EitaaOutboundService outboundService;

    public ScheduledMessageJob(JdbcTemplate jdbc, EitaaOutboundService outboundService) {
        this.jdbc = jdbc;
        this.outboundService = outboundService;
    }

    @Scheduled(fixedDelayString = "${app.eitaa.scheduler.fixed-delay-ms:30000}")
    public void processDueMessages() {
        Instant now = Instant.now();
        List<UUID> tenantIds = jdbc.query(
                """
                select distinct tenant_id from scheduled_messages
                where status = 'PENDING' and scheduled_at <= ?
                """,
                (rs, rowNum) -> rs.getObject("tenant_id", UUID.class),
                Timestamp.from(now));

        int total = 0;
        for (UUID tenantId : tenantIds) {
            TenantPrincipal principal = TenantPrincipal.ofTenantUser(
                    null, tenantId, null, Set.of(), MemberRole.TEACHER, "fa");
            Integer processed = TenantContext.callAs(principal,
                    () -> outboundService.processDueScheduledMessages(now));
            total += processed != null ? processed : 0;
        }
        if (total > 0) {
            log.info("جاب زمان‌بندی: {} پیام ارسال/شکست شد", total);
        }
    }
}
