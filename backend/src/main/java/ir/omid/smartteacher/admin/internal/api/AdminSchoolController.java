package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminSchoolResponse;
import ir.omid.smartteacher.admin.internal.api.dto.CreateAdminSchoolRequest;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateAdminSchoolRequest;
import ir.omid.smartteacher.admin.internal.application.AdminSchoolService;
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
 * مدیریت مدارس — نگاشت روی مستأجرهای {@code type=SCHOOL}.
 */
@RestController
@RequestMapping("/api/v1/admin/schools")
@Tag(name = "مدارس ادمین", description = "CRUD مدارس به‌عنوان مستأجر SCHOOL")
public class AdminSchoolController {

    private final AdminSchoolService schoolService;

    public AdminSchoolController(AdminSchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "فهرست مدارس")
    public PageResponse<AdminSchoolResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "اندازه صفحه")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "مرتب‌سازی، مثل createdAt,desc")
            @RequestParam(required = false) String sort) {
        return schoolService.list(page, size, sort);
    }

    @PostMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "ساخت مدرسه")
    public AdminSchoolResponse create(@Valid @RequestBody CreateAdminSchoolRequest request) {
        return schoolService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "جزئیات مدرسه")
    public AdminSchoolResponse get(@PathVariable UUID id) {
        return schoolService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "ویرایش مدرسه")
    public AdminSchoolResponse update(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateAdminSchoolRequest request) {
        return schoolService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف مدرسه")
    public void delete(@PathVariable UUID id) {
        schoolService.delete(id);
    }
}
