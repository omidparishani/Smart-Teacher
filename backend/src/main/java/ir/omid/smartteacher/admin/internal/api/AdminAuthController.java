package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminAuthResponse;
import ir.omid.smartteacher.admin.internal.api.dto.AdminLoginRequest;
import ir.omid.smartteacher.admin.internal.api.dto.AdminRefreshRequest;
import ir.omid.smartteacher.admin.internal.application.AdminAuthService;
import ir.omid.smartteacher.security.jwt.AuthTokenService;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.jwt.AdminTokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ورود، تازه‌سازی و خروج ادمین — {@code POST /api/v1/auth/login|refresh-admin|logout-admin}.
 *
 * <p>توکن‌ها در JSON (سازگاری تست) و کوکی HttpOnly ({@link AdminTokenCookies}) برمی‌گردند.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "احراز هویت ادمین", description = "ورود پنل مدیریت با نام کاربری و رمز")
public class AdminAuthController {

    private static final String HEADER_USER_AGENT = "User-Agent";

    private final AdminAuthService authService;
    private final JwtService jwtService;

    public AdminAuthController(AdminAuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "ورود ادمین با رمز",
            description = "نام کاربری و رمز را بررسی می‌کند و جفت توکن با دامنه ADMIN برمی‌گرداند. "
                    + "پس از ۵ تلاش ناموفق حساب ۱۵ دقیقه قفل می‌شود.")
    public AdminAuthResponse login(@Valid @RequestBody AdminLoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        AdminAuthResponse response = authService.login(
                request.username(),
                request.password(),
                request.totpCode(),
                clientInfoOf(httpRequest));
        attachTokenCookies(httpResponse, httpRequest, response);
        return response;
    }

    @PostMapping("/refresh-admin")
    @Operation(summary = "تازه‌سازی نشست ادمین",
            description = "توکن refresh از کوکی ST_ADMIN_REFRESH یا بدنه می‌آید.")
    public AdminAuthResponse refreshAdmin(@RequestBody(required = false) AdminRefreshRequest request,
                                          HttpServletRequest httpRequest,
                                          HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        AdminAuthResponse response = authService.refresh(refreshToken, clientInfoOf(httpRequest));
        attachTokenCookies(httpResponse, httpRequest, response);
        return response;
    }

    @PostMapping("/logout-admin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "خروج ادمین",
            description = "خانواده refresh را باطل می‌کند و کوکی‌های نشست را پاک می‌کند.")
    public void logoutAdmin(@RequestBody(required = false) AdminRefreshRequest request,
                            HttpServletRequest httpRequest,
                            HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        authService.logout(refreshToken);
        AdminTokenCookies.clear(httpResponse, httpRequest.isSecure());
    }

    private void attachTokenCookies(HttpServletResponse response,
                                    HttpServletRequest request,
                                    AdminAuthResponse auth) {
        AdminTokenCookies.attach(
                response,
                auth.accessToken(),
                auth.refreshToken(),
                jwtService.accessTokenTtlSeconds(),
                jwtService.refreshTokenTtl().toSeconds(),
                request.isSecure());
    }

    private static String resolveRefreshToken(AdminRefreshRequest request,
                                              HttpServletRequest httpRequest) {
        String fromCookie = AdminTokenCookies.readRefresh(httpRequest);
        if (fromCookie != null) {
            return fromCookie;
        }
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        return null;
    }

    private static AuthTokenService.ClientInfo clientInfoOf(HttpServletRequest request) {
        return new AuthTokenService.ClientInfo(
                request.getRemoteAddr(),
                request.getHeader(HEADER_USER_AGENT));
    }
}
