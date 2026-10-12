package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminDashboardResponse;
import ir.omid.smartteacher.admin.internal.application.AdminDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * داشبورد پایه ادمین — {@code GET /api/v1/admin/dashboard}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "داشبورد ادمین", description = "شمارنده‌های پلتفرم‌سطح")
public class AdminDashboardController {

    static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "داشبورد پایه",
            description = "شمارنده‌های واقعی کاربران، مستأجرها، کلاس‌ها و دانش‌آموزان. "
                    + "بخش‌های پیاده‌نشده (AI، درآمد، …) عمداً غایب‌اند.")
    public AdminDashboardResponse dashboard() {
        return dashboardService.dashboard();
    }
}
