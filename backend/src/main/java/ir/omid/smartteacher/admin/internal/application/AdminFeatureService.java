package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.CreateFeatureOverrideRequest;
import ir.omid.smartteacher.admin.internal.api.dto.FeatureOverrideResponse;
import ir.omid.smartteacher.admin.internal.api.dto.FeatureResponse;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateFeatureRequest;
import ir.omid.smartteacher.admin.internal.domain.Feature;
import ir.omid.smartteacher.admin.internal.domain.FeatureOverride;
import ir.omid.smartteacher.admin.internal.domain.FeatureOverrideRepository;
import ir.omid.smartteacher.admin.internal.domain.FeatureRepository;
import ir.omid.smartteacher.admin.internal.domain.UserFeatureOverride;
import ir.omid.smartteacher.admin.internal.domain.UserFeatureOverrideRepository;
import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantRepository;
import ir.omid.smartteacher.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feature Flags حداقلی فاز ۴ — کاتالوگ، پیش‌فرض، و override مستأجر/کاربر.
 *
 * <p>رزولوشن کامل entitlement و پلن در فاز صورتحساب می‌آید؛ اینجا فقط
 * مدیریت ادمین روی جداول V12 است.
 */
@Service
public class AdminFeatureService {

    private static final Logger log = LoggerFactory.getLogger(AdminFeatureService.class);

    private final FeatureRepository featureRepository;
    private final FeatureOverrideRepository featureOverrideRepository;
    private final UserFeatureOverrideRepository userFeatureOverrideRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SecurityAuditLog securityAuditLog;

    public AdminFeatureService(FeatureRepository featureRepository,
                               FeatureOverrideRepository featureOverrideRepository,
                               UserFeatureOverrideRepository userFeatureOverrideRepository,
                               TenantRepository tenantRepository,
                               UserRepository userRepository,
                               SecurityAuditLog securityAuditLog) {
        this.featureRepository = featureRepository;
        this.featureOverrideRepository = featureOverrideRepository;
        this.userFeatureOverrideRepository = userFeatureOverrideRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.securityAuditLog = securityAuditLog;
    }

    @Transactional(readOnly = true)
    public List<FeatureResponse> list() {
        return featureRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(AdminFeatureService::toFeatureResponse)
                .toList();
    }

    @Transactional
    public FeatureResponse create(ir.omid.smartteacher.admin.internal.api.dto.CreateFeatureRequest request) {
        String code = request.code().trim().toUpperCase();
        if (featureRepository.existsByCode(code)) {
            throw new DuplicateResourceException("امکان با این کد قبلاً ثبت شده است.");
        }
        Feature feature = new Feature(code, request.name().trim(), request.category().trim().toUpperCase());
        feature.setDescription(request.description() != null ? request.description().trim() : null);
        feature.setDefaultEnabled(request.defaultEnabled() != null && request.defaultEnabled());
        feature.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 100);
        featureRepository.save(feature);
        audit("FEATURE_CREATED", feature.getId(), null, Map.of("code", code));
        return toFeatureResponse(feature);
    }

    @Transactional
    public void deleteOverride(UUID id) {
        if (featureOverrideRepository.existsById(id)) {
            featureOverrideRepository.deleteById(id);
            audit("FEATURE_OVERRIDE_DELETED", id, null, Map.of("type", "TENANT"));
        } else if (userFeatureOverrideRepository.existsById(id)) {
            userFeatureOverrideRepository.deleteById(id);
            audit("FEATURE_OVERRIDE_DELETED", id, null, Map.of("type", "USER"));
        } else {
            throw ResourceNotFoundException.of("بازنویسی امکان");
        }
    }

    @Transactional
    public FeatureResponse update(String code, UpdateFeatureRequest request) {
        Feature feature = featureRepository.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("امکان"));

        if (request.defaultEnabled() == null
                && request.name() == null
                && request.description() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "حداقل یک فیلد برای به‌روزرسانی لازم است.");
        }

        Map<String, Object> before = featureSnapshot(feature);

        if (request.defaultEnabled() != null) {
            feature.setDefaultEnabled(request.defaultEnabled());
        }
        if (request.name() != null && !request.name().isBlank()) {
            feature.setName(request.name().trim());
        }
        if (request.description() != null) {
            feature.setDescription(request.description().isBlank()
                    ? null
                    : request.description().trim());
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("code", code);
        meta.put("before", before);
        meta.put("after", featureSnapshot(feature));
        audit("FEATURE_UPDATED", feature.getId(), null, meta);

        log.info("امکان {} توسط ادمین به‌روز شد", code);
        return toFeatureResponse(feature);
    }

    @Transactional
    public FeatureOverrideResponse upsertOverride(CreateFeatureOverrideRequest request) {
        String code = request.featureCode().trim();
        if (!featureRepository.existsByCode(code)) {
            throw ResourceNotFoundException.of("امکان");
        }

        UUID actorId = TenantContext.currentUserId().orElse(null);
        String scope = request.scope().trim();

        if ("TENANT".equals(scope)) {
            return upsertTenantOverride(request, code, actorId);
        }
        return upsertUserOverride(request, code, actorId);
    }

    private FeatureOverrideResponse upsertTenantOverride(CreateFeatureOverrideRequest request,
                                                         String code,
                                                         UUID actorId) {
        UUID tenantId = request.tenantId();
        if (tenantRepository.findByIdAndDeletedAtIsNull(tenantId).isEmpty()) {
            throw ResourceNotFoundException.of("مستأجر");
        }

        FeatureOverride override = featureOverrideRepository
                .findByTenantIdAndFeatureCode(tenantId, code)
                .orElseGet(() -> new FeatureOverride(tenantId, code, request.enabled()));

        override.setEnabled(request.enabled());
        override.setReason(blankToNull(request.reason()));
        override.setExpiresAt(request.expiresAt());
        override.setGrantedBy(actorId);
        featureOverrideRepository.save(override);

        Map<String, Object> overrideMeta = new LinkedHashMap<>();
        overrideMeta.put("featureCode", code);
        overrideMeta.put("tenantId", tenantId.toString());
        overrideMeta.put("enabled", request.enabled());
        audit("FEATURE_OVERRIDE_TENANT", override.getId(), tenantId, overrideMeta);

        log.info("بازنویسی امکان {} برای مستأجر {} ثبت شد", code, tenantId);
        return toTenantOverrideResponse(override);
    }

    private FeatureOverrideResponse upsertUserOverride(CreateFeatureOverrideRequest request,
                                                       String code,
                                                       UUID actorId) {
        UUID userId = request.userId();
        if (userRepository.findByIdAndDeletedAtIsNull(userId).isEmpty()) {
            throw ResourceNotFoundException.of("کاربر");
        }

        UserFeatureOverride override = userFeatureOverrideRepository
                .findByUserIdAndFeatureCode(userId, code)
                .orElseGet(() -> new UserFeatureOverride(userId, code, request.enabled()));

        override.setEnabled(request.enabled());
        override.setReason(blankToNull(request.reason()));
        override.setExpiresAt(request.expiresAt());
        override.setGrantedBy(actorId);
        userFeatureOverrideRepository.save(override);

        Map<String, Object> overrideMeta = new LinkedHashMap<>();
        overrideMeta.put("featureCode", code);
        overrideMeta.put("userId", userId.toString());
        overrideMeta.put("enabled", request.enabled());
        audit("FEATURE_OVERRIDE_USER", override.getId(), null, overrideMeta);

        log.info("بازنویسی امکان {} برای کاربر {} ثبت شد", code, userId);
        return toUserOverrideResponse(override);
    }

    private void audit(String detail, UUID entityId, UUID tenantId, Map<String, Object> metadata) {
        Map<String, Object> meta = new LinkedHashMap<>(metadata);
        meta.put("detail", detail);
        securityAuditLog.record(
                SecurityEvent.SENSITIVE_ACTION,
                tenantId,
                TenantContext.currentUserId().orElse(null),
                ActorType.USER,
                "Feature",
                entityId,
                meta);
    }

    private static Map<String, Object> featureSnapshot(Feature feature) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("defaultEnabled", feature.isDefaultEnabled());
        snap.put("name", feature.getName());
        snap.put("description", feature.getDescription());
        return snap;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static FeatureResponse toFeatureResponse(Feature feature) {
        return new FeatureResponse(
                feature.getId(),
                feature.getCode(),
                feature.getName(),
                feature.getCategory(),
                feature.getDescription(),
                feature.isDefaultEnabled(),
                feature.getSortOrder());
    }

    static FeatureOverrideResponse toTenantOverrideResponse(FeatureOverride override) {
        return new FeatureOverrideResponse(
                override.getId(),
                "TENANT",
                override.getTenantId(),
                null,
                override.getFeatureCode(),
                override.isEnabled(),
                override.getReason(),
                override.getExpiresAt(),
                override.getGrantedBy(),
                override.getCreatedAt());
    }

    static FeatureOverrideResponse toUserOverrideResponse(UserFeatureOverride override) {
        return new FeatureOverrideResponse(
                override.getId(),
                "USER",
                null,
                override.getUserId(),
                override.getFeatureCode(),
                override.isEnabled(),
                override.getReason(),
                override.getExpiresAt(),
                override.getGrantedBy(),
                override.getCreatedAt());
    }
}
