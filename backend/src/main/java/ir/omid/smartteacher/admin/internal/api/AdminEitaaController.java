package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminEitaaOverviewResponse;
import ir.omid.smartteacher.admin.internal.application.AdminEitaaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * نمای کلی ایتا برای ادمین — گروه‌ها، کانال‌ها، اعتبار پیام.
 */
@RestController
@RequestMapping("/api/v1/admin/eitaa")
@Tag(name = "ایتا ادمین", description = "مشاهده گروه‌ها، کانال‌ها و مصرف اعتبار")
public class AdminEitaaController {

    static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final AdminEitaaService eitaaService;

    public AdminEitaaController(AdminEitaaService eitaaService) {
        this.eitaaService = eitaaService;
    }

    @GetMapping("/overview")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "نمای کلی ایتا",
            description = "شمار گروه‌ها و کانال‌ها و مجموع اعتبار پیام — فقط خواندنی.")
    public AdminEitaaOverviewResponse overview() {
        return eitaaService.overview();
    }
}
