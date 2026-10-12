package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.AdminTenantResponse;
import ir.omid.smartteacher.admin.internal.api.dto.CreateAdminTenantRequest;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateAdminTenantRequest;
import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.tenant.Tenant;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantRepository;
import ir.omid.smartteacher.tenant.TenantStatus;
import ir.omid.smartteacher.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * مدیریت مستأجرها در پنل ادمین — داده واقعی از جدول {@code tenants}.
 *
 * <p>جداسازی: این Endpointها پلتفرم‌سطح‌اند و فقط با نقش ادمین در دسترس‌اند.
 * معلم به آن‌ها دسترسی ندارد (تست جداسازی).
 */
@Service
public class AdminTenantService {

    private static final Logger log = LoggerFactory.getLogger(AdminTenantService.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "name", "slug", "status", "type");

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SecurityAuditLog securityAuditLog;

    public AdminTenantService(TenantRepository tenantRepository,
                              UserRepository userRepository,
                              SecurityAuditLog securityAuditLog) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.securityAuditLog = securityAuditLog;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminTenantResponse> list(int page, int size, String sort) {
        Pageable pageable = PageableFactory.of(
                page, size, sort, ALLOWED_SORT_FIELDS, "createdAt,desc");
        Page<Tenant> tenants = tenantRepository.findByDeletedAtIsNull(pageable);
        return PageResponse.from(tenants, AdminTenantService::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminTenantResponse get(UUID tenantId) {
        return toResponse(requireTenant(tenantId));
    }

    @Transactional
    public AdminTenantResponse create(CreateAdminTenantRequest request) {
        String slug = request.slug().trim();
        if (tenantRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE,
                    "این نامک قبلاً ثبت شده است.");
        }

        if (userRepository.findByIdAndDeletedAtIsNull(request.ownerUserId()).isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "مالک مستأجر یافت نشد.");
        }

        UUID parentId = request.parentTenantId();
        if (parentId != null) {
            requireTenant(parentId);
        }

        String settings = normalizeSettings(request.settings());

        Tenant tenant = new Tenant(
                request.type(),
                request.name().trim(),
                slug,
                request.ownerUserId());
        tenant.setParentTenantId(parentId);
        tenant.setSettings(settings);
        tenantRepository.save(tenant);

        audit("TENANT_CREATED", tenant.getId(), Map.of(
                "slug", slug,
                "type", request.type().name(),
                "ownerUserId", request.ownerUserId().toString()));

        log.info("مستأجر {} ({}) توسط ادمین ساخته شد", tenant.getId(), slug);
        return toResponse(tenant);
    }

    @Transactional
    public AdminTenantResponse update(UUID tenantId, UpdateAdminTenantRequest request) {
        Tenant tenant = requireTenant(tenantId);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("name", tenant.getName());
        before.put("status", tenant.getStatus().name());

        tenant.setName(request.name().trim());
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }
        if (request.settings() != null) {
            tenant.setSettings(normalizeSettings(request.settings()));
        }

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("name", tenant.getName());
        after.put("status", tenant.getStatus().name());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("before", before);
        meta.put("after", after);
        audit("TENANT_UPDATED", tenantId, meta);

        log.info("مستأجر {} توسط ادمین به‌روز شد", tenantId);
        return toResponse(tenant);
    }

    @Transactional
    public void delete(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        tenant.setDeletedAt(java.time.Instant.now());
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);

        audit("TENANT_DELETED", tenantId, Map.of(
                "slug", tenant.getSlug(),
                "type", tenant.getType().name()
        ));
        log.info("مستأجر {} توسط ادمین حذف نرم شد", tenantId);
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("مستأجر"));
    }

    private static String normalizeSettings(String settings) {
        if (settings == null || settings.isBlank()) {
            return "{}";
        }
        String trimmed = settings.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "تنظیمات باید یک شیء JSON معتبر باشد.");
        }
        return trimmed;
    }

    private void audit(String detail, UUID entityId, Map<String, Object> metadata) {
        Map<String, Object> meta = new LinkedHashMap<>(metadata);
        meta.put("detail", detail);
        securityAuditLog.record(
                SecurityEvent.SENSITIVE_ACTION,
                entityId,
                TenantContext.currentUserId().orElse(null),
                ActorType.USER,
                "Tenant",
                entityId,
                meta);
    }

    static AdminTenantResponse toResponse(Tenant tenant) {
        return new AdminTenantResponse(
                tenant.getId(),
                tenant.getType(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getParentTenantId(),
                tenant.getOwnerUserId(),
                tenant.getStatus(),
                tenant.getSettings(),
                tenant.getCreatedAt());
    }
}
