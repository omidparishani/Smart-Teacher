package ir.omid.smartteacher.security.ratelimit;

import ir.omid.smartteacher.common.error.ApiError;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.security.web.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * محدودیت نرخ روی {@code POST /api/v1/auth/login} بر اساس IP + username.
 *
 * <p>بدنه یک‌بار خوانده و با {@link CachedBodyHttpServletRequest} دوباره
 * در اختیار کنترلر گذاشته می‌شود.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final java.util.Set<String> MONITORED_PATHS = java.util.Set.of(
            LOGIN_PATH,
            "/api/v1/auth/eitaa",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh-admin",
            "/api/v1/auth/logout-admin"
    );

    private final RateLimiter rateLimiter;
    private final LoginRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public LoginRateLimitFilter(RateLimiter rateLimiter,
                                LoginRateLimitProperties properties,
                                ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !MONITORED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (LOGIN_PATH.equals(uri)) {
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);

            String username = extractUsername(body);
            String ip = clientIp(wrapped);
            String key = "login:" + ip + "|" + username.toLowerCase(Locale.ROOT);

            if (!rateLimiter.tryAcquire(key)) {
                writeTooManyRequests(wrapped, response);
                return;
            }
            filterChain.doFilter(wrapped, response);
            return;
        }

        // ⚠️ سایر مسیرهای احراز هویت (eitaa, refresh, logout و ...) — سهمیه بر اساس IP و مسیر
        String ip = clientIp(request);
        String key = "auth:" + uri + "|" + ip;

        if (!rateLimiter.tryAcquire(key)) {
            writeTooManyRequests(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String extractUsername(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode username = root.get("username");
            return username == null || username.isNull() ? "" : username.asString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ErrorCode code = ErrorCode.RATE_LIMIT_EXCEEDED;
        response.setStatus(code.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiError body = ApiError.of(code, code.defaultMessage(),
                JwtAuthenticationFilter.resolveTraceId(request), request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
