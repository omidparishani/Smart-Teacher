package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminUserResponse;
import ir.omid.smartteacher.admin.internal.api.dto.ChangeRoleRequest;
import ir.omid.smartteacher.admin.internal.api.dto.CreateAdminUserRequest;
import ir.omid.smartteacher.admin.internal.api.dto.ResetPasswordRequest;
import ir.omid.smartteacher.admin.internal.api.dto.SuspendUserRequest;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateAdminUserRequest;
import ir.omid.smartteacher.admin.internal.application.AdminUserService;
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
 * مدیریت کاربران ادمین — فهرست، جزئیات، ساخت، ویرایش، وضعیت، نقش و رمز.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "کاربران ادمین", description = "مدیریت کاربران پلتفرم")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "فهرست کاربران",
            description = "کاربران حذف‌نشده را صفحه‌بندی‌شده برمی‌گرداند. بدون هش رمز.")
    public PageResponse<AdminUserResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "اندازه صفحه")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "مرتب‌سازی، مثل createdAt,desc")
            @RequestParam(required = false) String sort) {
        return userService.list(page, size, sort);
    }

    @PostMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "ساخت کاربر ستاد",
            description = "کاربر ادمین/ستاد با رمز و نقش پلتفرمی می‌سازد. رمز در پاسخ نیست.")
    public AdminUserResponse create(@Valid @RequestBody CreateAdminUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "جزئیات کاربر",
            description = "یک کاربر حذف‌نشده را با نقش‌های پلتفرمی برمی‌گرداند.")
    public AdminUserResponse get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "ویرایش پروفایل کاربر",
            description = "نام، ایمیل و موبایل را به‌روز می‌کند. رمز و نقش مسیر جدا دارند.")
    public AdminUserResponse update(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateAdminUserRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "غیرفعال‌سازی کاربر",
            description = "وضعیت را DISABLED می‌کند و همه نشست‌ها را باطل می‌کند.")
    public AdminUserResponse disable(@PathVariable UUID id) {
        return userService.disable(id);
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "فعال‌سازی کاربر",
            description = "حساب غیرفعال یا معلق را دوباره ACTIVE می‌کند.")
    public AdminUserResponse enable(@PathVariable UUID id) {
        return userService.enable(id);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "تعلیق کاربر",
            description = "وضعیت را SUSPENDED می‌کند؛ دلیل در حسابرسی ثبت می‌شود.")
    public AdminUserResponse suspend(@PathVariable UUID id,
                                     @Valid @RequestBody SuspendUserRequest request) {
        return userService.suspend(id, request.reason());
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "بازنشانی رمز",
            description = "رمز جدید می‌گذارد و همه نشست‌ها را باطل می‌کند. رمز در پاسخ نیست.")
    public AdminUserResponse resetPassword(@PathVariable UUID id,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(id, request);
    }

    @PostMapping("/{id}/change-role")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "تغییر نقش پلتفرمی",
            description = "نقش SUPER_ADMIN یا ADMIN را جایگزین می‌کند. ادمین نقش خودش را عوض نمی‌کند.")
    public AdminUserResponse changeRole(@PathVariable UUID id,
                                        @Valid @RequestBody ChangeRoleRequest request) {
        return userService.changeRole(id, request);
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "دریافت نقش‌های پلتفرمی کاربر")
    public java.util.List<String> getUserRoles(@PathVariable UUID id) {
        return userService.get(id).platformRoles();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "به‌روزرسانی نقش پلتفرمی کاربر")
    public AdminUserResponse updateRoles(@PathVariable UUID id,
                                         @Valid @RequestBody ChangeRoleRequest request) {
        return userService.changeRole(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف نرم کاربر",
            description = "deleted_at می‌گذارد، حساب را DISABLED می‌کند و نشست‌ها را باطل می‌کند. "
                    + "ادمین نمی‌تواند خودش را حذف کند.")
    public void softDelete(@PathVariable UUID id) {
        userService.softDelete(id);
    }
}
