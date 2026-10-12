package ir.omid.smartteacher.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * کوکی‌های HttpOnly نشست ادمین — مکمل هدر Bearer برای پنل SPA.
 *
 * <p>در {@code security::jwt} است تا ماژول admin بتواند بدون نقض Modulith
 * از آن استفاده کند.
 */
public final class AdminTokenCookies {

    public static final String ACCESS_COOKIE = "ST_ADMIN_ACCESS";
    public static final String REFRESH_COOKIE = "ST_ADMIN_REFRESH";

    private static final String COOKIE_PATH = "/api/v1";

    private AdminTokenCookies() {
    }

    public static void attach(HttpServletResponse response,
                              String accessToken,
                              String refreshToken,
                              long accessMaxAgeSeconds,
                              long refreshMaxAgeSeconds,
                              boolean secure) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(ACCESS_COOKIE, accessToken, accessMaxAgeSeconds, secure).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(REFRESH_COOKIE, refreshToken, refreshMaxAgeSeconds, secure).toString());
    }

    public static void clear(HttpServletResponse response, boolean secure) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(ACCESS_COOKIE, "", 0, secure).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(REFRESH_COOKIE, "", 0, secure).toString());
    }

    public static String readAccess(HttpServletRequest request) {
        return readCookie(request, ACCESS_COOKIE);
    }

    public static String readRefresh(HttpServletRequest request) {
        return readCookie(request, REFRESH_COOKIE);
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value == null || value.isBlank()) ? null : value.trim();
            }
        }
        return null;
    }

    private static ResponseCookie buildCookie(String name,
                                                String value,
                                                long maxAgeSeconds,
                                                boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
