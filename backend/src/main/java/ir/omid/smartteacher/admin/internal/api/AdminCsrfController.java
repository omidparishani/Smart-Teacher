package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.CsrfTokenResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * صدور توکن CSRF برای SPA — {@code GET /api/v1/admin/csrf}.
 *
 * <p>کلاینت SPA توکن را از JSON می‌خواند و در هدر mutating می‌گذارد.
 * درخواست‌های Bearer همچنان CSRF را نادیده می‌گیرند (سازگاری تست).
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "CSRF ادمین", description = "توکن CSRF برای کلاینت SPA")
public class AdminCsrfController {

    @GetMapping("/csrf")
    @Operation(summary = "دریافت توکن CSRF",
            description = "توکن و نام هدر را برمی‌گرداند؛ کوکی XSRF-TOKEN هم ست می‌شود.")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName());
    }
}
