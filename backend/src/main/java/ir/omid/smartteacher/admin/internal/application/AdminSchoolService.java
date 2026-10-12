package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.AdminSchoolResponse;
import ir.omid.smartteacher.admin.internal.api.dto.CreateAdminSchoolRequest;
import ir.omid.smartteacher.admin.internal.api.dto.CreateAdminTenantRequest;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateAdminSchoolRequest;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateAdminTenantRequest;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.tenant.Tenant;
import ir.omid.smartteacher.tenant.TenantRepository;
import ir.omid.smartteacher.tenant.TenantType;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * مدیریت مدارس — لایه نازک روی مستأجرهای {@link TenantType#SCHOOL}.
 *
 * <p>جدول جدا برای مدرسه ساخته نشد؛ همان {@code tenants} با نوع SCHOOL
 * منبع حقیقت است تا سلسله‌مراتب والد/فرزند و مالکیت یکدست بماند.
 */
@Service
public class AdminSchoolService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "name", "slug", "status");

    private final AdminTenantService tenantService;
    private final TenantRepository tenantRepository;

    public AdminSchoolService(AdminTenantService tenantService,
                              TenantRepository tenantRepository) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSchoolResponse> list(int page, int size, String sort) {
        Pageable pageable = PageableFactory.of(
                page, size, sort, ALLOWED_SORT_FIELDS, "createdAt,desc");
        Page<Tenant> schools = tenantRepository.findByTypeAndDeletedAtIsNull(
                TenantType.SCHOOL, pageable);
        return PageResponse.from(schools, AdminSchoolService::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminSchoolResponse get(UUID schoolId) {
        return toResponse(requireSchool(schoolId));
    }

    @Transactional
    public AdminSchoolResponse create(CreateAdminSchoolRequest request) {
        var tenant = tenantService.create(new CreateAdminTenantRequest(
                TenantType.SCHOOL,
                request.name(),
                request.slug(),
                request.ownerUserId(),
                request.parentTenantId(),
                request.settings()));
        return new AdminSchoolResponse(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.parentTenantId(),
                tenant.ownerUserId(),
                tenant.status(),
                tenant.settings(),
                tenant.createdAt());
    }

    @Transactional
    public AdminSchoolResponse update(UUID schoolId, UpdateAdminSchoolRequest request) {
        requireSchool(schoolId);
        var tenant = tenantService.update(schoolId, new UpdateAdminTenantRequest(
                request.name(), request.status(), request.settings()));
        return new AdminSchoolResponse(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.parentTenantId(),
                tenant.ownerUserId(),
                tenant.status(),
                tenant.settings(),
                tenant.createdAt());
    }

    @Transactional
    public void delete(UUID schoolId) {
        requireSchool(schoolId);
        tenantService.delete(schoolId);
    }

    private Tenant requireSchool(UUID schoolId) {
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("مدرسه"));
        if (tenant.getType() != TenantType.SCHOOL) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "این مستأجر مدرسه نیست.");
        }
        return tenant;
    }

    private static AdminSchoolResponse toResponse(Tenant tenant) {
        return new AdminSchoolResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getParentTenantId(),
                tenant.getOwnerUserId(),
                tenant.getStatus(),
                tenant.getSettings(),
                tenant.getCreatedAt());
    }
}
