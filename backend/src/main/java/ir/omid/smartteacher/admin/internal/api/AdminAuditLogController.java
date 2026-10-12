package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminAuditLogResponse;
import ir.omid.smartteacher.audit.AuditLog;
import ir.omid.smartteacher.audit.AuditLogRepository;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * مشاهده لاگ‌های حسابرسی در پنل مدیریت.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "لاگ‌های حسابرسی ادمین", description = "مشاهده لاگ‌های حسابرسی و امنیتی پلتفرم")
public class AdminAuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping({"/audit-log", "/audit-logs"})
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "فهرست لاگ‌های حسابرسی")
    public PageResponse<AdminAuditLogResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID tenantId) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<AuditLog> pageResult;
        if (tenantId != null) {
            pageResult = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        } else {
            pageResult = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return PageResponse.from(pageResult.map(this::toResponse));
    }

    private AdminAuditLogResponse toResponse(AuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getTenantId(),
                log.getActorUserId(),
                log.getActorType(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getTraceId(),
                log.getSeverity(),
                log.getCreatedAt()
        );
    }
}
