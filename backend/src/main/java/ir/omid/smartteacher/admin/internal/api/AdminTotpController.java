package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.application.AdminTotpService;
import ir.omid.smartteacher.admin.internal.application.AdminTotpService.TotpSetupResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * فعال‌سازی 2FA سوپرادمین.
 */
@RestController
@RequestMapping("/api/v1/admin/2fa")
@Tag(name = "2FA ادمین", description = "TOTP برای سوپرادمین")
public class AdminTotpController {

    private final AdminTotpService totpService;

    public AdminTotpController(AdminTotpService totpService) {
        this.totpService = totpService;
    }

    @PostMapping("/setup")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "شروع فعال‌سازی 2FA — برگرداندن secret")
    public TotpSetupResponse setup() {
        return totpService.beginSetup();
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "تأیید کد و فعال‌سازی 2FA")
    public void confirm(@RequestBody ConfirmRequest request) {
        totpService.confirmSetup(request.code());
    }

    public record ConfirmRequest(@NotBlank String code) {
    }
}
