package ir.omid.smartteacher.security.web;

import ir.omid.smartteacher.common.error.InvalidTokenException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.jwt.AdminTokenCookies;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.rbac.RolePermissionAuthorityLoader;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * استخراج هویت از توکن و برقراری زمینه مستأجر برای طول درخواست.
 *
 * <p>این فیلتر دو کار را با هم انجام می‌دهد چون هر دو از یک منبع
 * تغذیه می‌شوند و جدا کردنشان یعنی دو بار خواندن و اعتبارسنجی توکن:
 * <ol>
 *   <li>اعتبارسنجی توکن و قرار دادن هویت در {@code SecurityContext}
 *       تا Spring Security بتواند تصمیم‌های دسترسی را بگیرد.</li>
 *   <li>برقراری {@link TenantContext} برای طول درخواست، تا لایه
 *       ماندگاری بتواند کوئری‌ها را به مستأجر جاری محدود کند.</li>
 * </ol>
 *
 * <p><b>⚠️ قاعده بحرانی:</b> توکن دسترسی ابتدا از هدر {@code Authorization}
 * خوانده می‌شود. اگر نبود، کوکی HttpOnly {@link AdminTokenCookies#ACCESS_COOKIE}
 * برای نشست پنل ادمین بررسی می‌شود (دوگانه: Bearer یا کوکی). هرگز از
 * پارامتر پرس‌وجو — توکن داخل URL در Log و Referer ثبت می‌شود.
 *
 * <p><b>⚠️ چرا توکن نامعتبر استثنا پرتاب نمی‌کند؟</b> چون استثنای
 * پرتاب‌شده در فیلتر توسط {@code @RestControllerAdvice} گرفته
 * <i>نمی‌شود</i> — آن فقط داخل DispatcherServlet کار می‌کند. اگر پرتاب
 * کنیم، کاربر به‌جای بدنه JSON استاندارد، یک صفحه خطای پیش‌فرض
 * کانتینر می‌بیند. پس دلیل شکست در یک attribute درخواست ثبت می‌شود و
 * به {@code JsonAuthenticationEntryPoint} سپرده می‌شود تا پاسخ درست را
 * بسازد.
 *
 * <p>در هر دو حالت — توکن معتبر و نامعتبر — یک زمینه در
 * {@link TenantContext} برقرار می‌شود. برای درخواست بدون توکن، زمینه
 * «کاربر ناشناس» است. این یعنی هیچ کدی داخل یک درخواست HTTP بدون
 * زمینه اجرا نمی‌شود و لازم نیست هر جا «شاید زمینه نباشد» را بررسی کند.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** نام attribute‌ای که دلیل شکست احراز هویت را نگه می‌دارد. */
    static final String ATTRIBUTE_FAILURE_REASON = "security.failureReason";

    private static final String ATTRIBUTE_TRACE_ID = "security.traceId";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String MDC_TRACE_ID = "traceId";

    /** پیشوند نقشی که به عضویت داخل مستأجر اشاره دارد. */
    private static final String TENANT_ROLE_PREFIX = "TENANT_";

    private final JwtService jwtService;
    private final RolePermissionAuthorityLoader permissionLoader;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   RolePermissionAuthorityLoader permissionLoader) {
        this.jwtService = jwtService;
        this.permissionLoader = permissionLoader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        TenantPrincipal principal = TenantPrincipal.anonymous();

        String token = extractAccessToken(request);
        if (token != null) {
            try {
                principal = jwtService.parseAccessToken(token);
                populateSecurityContext(principal);
            } catch (InvalidTokenException e) {
                // ⚠️ دلیل دقیق برای کاربر فرستاده نمی‌شود؛ فقط پاسخ ۴۰۱
                // (یا ۴۰۱ با پیام «نشست منقضی») ساخته می‌شود.
                request.setAttribute(ATTRIBUTE_FAILURE_REASON, e.getErrorCode());
                principal = TenantPrincipal.anonymous();
            }
        }

        try {
            TenantContext.runAsChecked(principal,
                    () -> filterChain.doFilter(request, response));
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            // runAsChecked استثناهای checked را دست‌نخورده رد می‌کند؛
            // این شاخه فقط برای خطاهای غیرمنتظره موتور ScopedValue است.
            throw new ServletException(e);
        }
    }

    private static String extractAccessToken(HttpServletRequest request) {
        String bearer = extractBearerToken(request);
        if (bearer != null) {
            return bearer;
        }
        return AdminTokenCookies.readAccess(request);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || header.length() <= BEARER_PREFIX.length()) {
            return null;
        }
        // مقایسه بدون حساسیت به بزرگی/کوچکی حروف، چون برخی کلاینت‌ها
        // «bearer» می‌فرستند و رد کردنشان بی‌دلیل است.
        if (!header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * قرار دادن هویت در {@code SecurityContext}.
     *
     * <p>مقام‌ها با کد خام نقش ساخته می‌شوند (مثل {@code TEACHER}) و نه
     * با پیشوند {@code ROLE_}، تا در {@code @PreAuthorize} بتوان
     * {@code hasAuthority('TEACHER')} نوشت — همان سبکی که
     * {@code docs/security.md} برای مجوزها استفاده می‌کند.
     *
     * <p>نقش‌ها و مجوزهای {@code role_permissions} هر دو به {@code GrantedAuthority}
     * تبدیل می‌شوند تا {@code @PreAuthorize} بتواند از هر دو استفاده کند.
     */
    private void populateSecurityContext(TenantPrincipal principal) {
        List<GrantedAuthority> authorities = new ArrayList<>(principal.roles().size() + 8);

        for (String role : principal.roles()) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        for (String permission : permissionLoader.permissionsForRoles(principal.roles())) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        if (principal.tenantRole() != null) {
            authorities.add(new SimpleGrantedAuthority(
                    TENANT_ROLE_PREFIX + principal.tenantRole().name()));
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    /**
     * شناسه ردیابی برای همبستگی پاسخ خطا با Log.
     *
     * <p>یک بار ساخته و در attribute درخواست نگه داشته می‌شود تا اگر
     * چند نقطه (entry point، access denied handler) آن را بخواهند، همه
     * یک مقدار بگیرند.
     */
    /** شناسه ردیابی مشترک برای فیلترها و handlerهای امنیتی خارج از این بسته. */
    public static String resolveTraceId(HttpServletRequest request) {
        Object existing = request.getAttribute(ATTRIBUTE_TRACE_ID);
        if (existing instanceof String traceId && !traceId.isBlank()) {
            return traceId;
        }

        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UuidV7Generator.generate().toString();
        }

        request.setAttribute(ATTRIBUTE_TRACE_ID, traceId);
        return traceId;
    }
}
