package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminTenantResponse;
import ir.omid.smartteacher.admin.internal.api.dto.CreateAdminTenantRequest;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateAdminTenantRequest;
import ir.omid.smartteacher.admin.internal.application.AdminTenantService;
import ir.omid.smartteacher.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مدیریت مستأجرها — داده واقعی از جدول {@code tenants}.
 */
@RestController
@RequestMapping("/api/v1/admin/tenants")
@Tag(name = "مستأجرهای ادمین", description = "فهرست و مدیریت مستأجرهای پلتفرم")
public class AdminTenantController {

    private final AdminTenantService tenantService;

    public AdminTenantController(AdminTenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "فهرست مستأجرها")
    public PageResponse<AdminTenantResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "اندازه صفحه")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "مرتب‌سازی، مثل createdAt,desc")
            @RequestParam(required = false) String sort) {
        return tenantService.list(page, size, sort);
    }

    @PostMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "ساخت مستأجر")
    public AdminTenantResponse create(@Valid @RequestBody CreateAdminTenantRequest request) {
        return tenantService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "جزئیات مستأجر")
    public AdminTenantResponse get(@PathVariable UUID id) {
        return tenantService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "ویرایش مستأجر")
    public AdminTenantResponse update(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateAdminTenantRequest request) {
        return tenantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف مستأجر")
    public void delete(@PathVariable UUID id) {
        tenantService.delete(id);
    }
}
