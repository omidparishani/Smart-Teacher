package ir.omid.smartteacher.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به لاگ حسابرسی.
 *
 * <p>فیلتر خودکار مستأجر ندارد — دلیلش در Javadoc خود {@link AuditLog}.
 * پس <b>هر</b> پرس‌وجویی که نتیجه‌اش به کاربر نشان داده می‌شود باید شرط
 * {@code tenant_id} داشته باشد؛ اینجا با متدهایی که {@code tenantId} را
 * در نامشان دارند تضمین شده. متدهایی که {@code tenantId} ندارند فقط
 * برای ادمین پلتفرم و پایش هستند.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(UUID tenantId,
                                                              String action,
                                                              Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    List<AuditLog> findByTraceId(String traceId);

    /** پایش امنیتی — رویدادهای مهم بازه اخیر. */
    List<AuditLog> findBySeverityInAndCreatedAtAfterOrderByCreatedAtDesc(List<Severity> severities,
                                                                        Instant since);

    /** تعداد رویدادهای مهم یک مستأجر — برای نمایش هشدار در داشبورد. */
    long countByTenantIdAndSeverity(UUID tenantId, Severity severity);
}
