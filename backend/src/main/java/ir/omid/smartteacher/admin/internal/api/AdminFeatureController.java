package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.CreateFeatureOverrideRequest;
import ir.omid.smartteacher.admin.internal.api.dto.FeatureOverrideResponse;
import ir.omid.smartteacher.admin.internal.api.dto.FeatureResponse;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateFeatureRequest;
import ir.omid.smartteacher.admin.internal.application.AdminFeatureService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature Flags — کاتالوگ و بازنویسی مستأجر/کاربر.
 */
@RestController
@RequestMapping("/api/v1/admin/features")
@Tag(name = "امکانات ادمین", description = "کاتالوگ Feature Flags و بازنویسی‌ها")
public class AdminFeatureController {

    private final AdminFeatureService featureService;

    public AdminFeatureController(AdminFeatureService featureService) {
        this.featureService = featureService;
    }

    @GetMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "فهرست امکانات",
            description = "کاتالوگ کامل امکانات با پیش‌فرض فعال بودن.")
    public List<FeatureResponse> list() {
        return featureService.list();
    }

    @PutMapping("/{code}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "به‌روزرسانی امکان",
            description = "پیش‌فرض فعال بودن و فراداده نمایشی را عوض می‌کند.")
    public FeatureResponse update(@PathVariable String code,
                                  @Valid @RequestBody UpdateFeatureRequest request) {
        return featureService.update(code, request);
    }

    @PostMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "افزودن امکان جدید")
    public FeatureResponse create(@Valid @RequestBody ir.omid.smartteacher.admin.internal.api.dto.CreateFeatureRequest request) {
        return featureService.create(request);
    }

    @PostMapping("/overrides")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "بازنویسی امکان",
            description = "برای مستأجر یا کاربر یک override می‌سازد یا جایگزین می‌کند.")
    public FeatureOverrideResponse createOverride(
            @Valid @RequestBody CreateFeatureOverrideRequest request) {
        return featureService.upsertOverride(request);
    }

    @DeleteMapping("/overrides/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف بازنویسی امکان")
    public void deleteOverride(@PathVariable UUID id) {
        featureService.deleteOverride(id);
    }
}
