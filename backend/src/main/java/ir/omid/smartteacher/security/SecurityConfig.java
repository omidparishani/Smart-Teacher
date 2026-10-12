package ir.omid.smartteacher.security;

import ir.omid.smartteacher.security.web.JsonAccessDeniedHandler;
import ir.omid.smartteacher.security.web.JsonAuthenticationEntryPoint;
import ir.omid.smartteacher.security.web.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * پیکربندی امنیت وب — دو زنجیره فیلتر جدا.
 *
 * <p>دو مسیر احراز هویت ({@code docs/security.md} بخش ۲) دو سطح ریسک
 * متفاوت دارند و نباید یک پیکربندی مشترک داشته باشند:
 *
 * <table border="1">
 *   <caption>تفاوت دو زنجیره</caption>
 *   <tr><th></th><th>Mini App (معلم و دانش‌آموز)</th><th>پنل ادمین</th></tr>
 *   <tr><td>مسیر</td><td>بقیه مسیرها</td><td>{@code /api/v1/admin/**} + ورود ادمین</td></tr>
 *   <tr><td>محل توکن</td><td>هدر {@code Authorization}</td><td>کوکی {@code HttpOnly}</td></tr>
 *   <tr><td>CSRF</td><td>خاموش — توکن در هدر است، نه کوکی</td><td>روشن — کوکی خودکار فرستاده می‌شود</td></tr>
 * </table>
 *
 * <p><b>چرا CSRF در پنل ادمین اجباری است؟</b> چون مرورگر کوکی را
 * خودکار به هر درخواست به دامنه ما ضمیمه می‌کند. بدون توکن CSRF، یک
 * صفحه مخرب می‌تواند مرورگر مدیر وارد‌شده را وادار کند عملیاتی را اجرا
 * کند. در Mini App توکن در هدر است و مرورگر آن را خودکار نمی‌فرستد،
 * پس حمله CSRF ممکن نیست و خاموش‌کردنش سطح حمله اضافه نمی‌سازد.
 *
 * <p><b>ترتیب زنجیره‌ها مهم است:</b> زنجیره ادمین با {@code @Order(1)}
 * اول می‌آید، وگرنه زنجیره عمومی — که {@code anyRequest()} دارد —
 * مسیرهای ادمین را هم می‌بلعد و CSRF خاموش می‌ماند.
 *
 * <p><b>⚠️ چرا هیچ مکانیزم ورودی اینجا ثبت نشده؟</b> عمدی است. نه
 * {@code formLogin()} صدا زده می‌شود و نه {@code httpBasic()} و نه
 * {@code oauth2Login()}. احراز هویت <i>فقط</i> از راه توکن انجام
 * می‌شود که {@code JwtAuthenticationFilter} آن را از هدر می‌خواند.
 *
 * <p>پیامدش این است که Spring Boot ممکن است هنگام بالا آمدن، یک کاربر
 * پیش‌فرض در حافظه بسازد و رمز تصادفی‌اش را در Log چاپ کند
 * («Using generated security password: ...»). آن پیام <b>بی‌خطر</b>
 * است: هیچ مسیر ورودی وجود ندارد که آن کاربر از آن استفاده شود. اگر
 * روزی کسی {@code formLogin()} یا {@code httpBasic()} را اضافه کرد،
 * همین کاربر پیش‌فرض به یک مسیر ورود واقعی وصل می‌شود — پس آن تغییر
 * نیازمند بازبینی امنیتی است، نه یک تنظیم ساده.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * مسیرهای احراز هویتِ برنامک که بدون توکن دسترسی کار می‌کنند.
     *
     * <p><b>🔴 چرا این فهرست باریک است و «{@code /api/v1/auth/**}» نیست؟</b>
     * نسخه پیشین این کلاس مسیر را با wildcard باز می‌گذاشت و همین کافی
     * بود تا {@code /auth/me} و {@code /auth/logout} هم بدون احراز هویت
     * در دسترس باشند — یعنی هر کسی می‌توانست پروفایل دیگری را بخواند
     * یا نشستش را ببندد، اگر روزی مسیرها به آن سمت می‌رفتند. بازکردن
     * یک <i>مسیر تازه</i> زیر یک wildcard باز، بدون هیچ هشداری، آن را
     * عمومی می‌کند.
     *
     * <p>هر مسیر اینجا باید دلیل مستقل داشته باشد:
     * <ul>
     *   <li>{@code /auth/eitaa} — قبل از ورود، توکنی وجود ندارد که
     *       فرستاده شود. {@code initData} جایگزین آن است.</li>
     *   <li>{@code /auth/refresh} — کلاینتی که توکن دسترسی‌اش منقضی
     *       شده باید بتواند تازه‌سازی کند؛ توکن تازه‌سازی خودش مدرک
     *       است و در بدنه می‌آید.</li>
     * </ul>
     *
     * <p>⚠️ {@code /auth/logout} عمداً اینجا نیست. توکن تازه‌سازی در
     * بدنه‌اش می‌آید، ولی توکن دسترسی هم لازم است — و این سخت‌گیری
     * بی‌معنا نیست: پنجره‌ای که یک توکن دزدیده‌شده بتواند نشست را ببندد
     * کوتاه‌تر می‌شود، و کلاینت معتبر هم می‌تواند ابتدا تازه‌سازی کند.
     *
     * <p>⚠️ {@code /auth/login} اینجا نیست چون در زنجیره ادمین
     * ({@link #adminSecurityFilterChain}) با {@code permitAll} باز
     * شده — مطابق {@code docs/security.md} بخش ۵.
     */
    private static final String[] PUBLIC_AUTH_PATHS = {
            "/api/v1/auth/eitaa",
            "/api/v1/auth/refresh"
    };

    /** مسیرهایی که بدون ورود قابل دسترسی‌اند. */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/public/**",
            "/api/v1/plans",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // ⚠️ مسیر /error باید باز باشد. بدون آن، وقتی خطایی رخ می‌دهد
            // Spring Boot برای نمایش آن به /error فوروارد می‌کند و آن
            // درخواست داخلی ۴۰۱ می‌گیرد — یعنی کاربر به‌جای خطای واقعی،
            // یک ۴۰۱ گمراه‌کننده می‌بیند و عیب‌یابی غیرممکن می‌شود.
            "/error"
    };

    /**
     * زنجیره پنل مدیریت — کوکی، CSRF روشن، نقش ادمین الزامی.
     *
     * <p>مطابق {@code docs/security.md} بخش ۵: علاوه بر
     * {@code /api/v1/admin/**}، مسیرهای {@code /auth/login} و
     * {@code /auth/refresh-admin} هم اینجا هستند تا CSRF و قواعد نقش
     * ادمین روی همان زنجیره اعمال شود.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) throws Exception {

        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // بدون deferred، کوکی CSRF در همان درخواست GET /csrf ست می‌شود
        // (رفتار پیش‌فرض Spring Security 6 گاهی صدور را به تعویق می‌اندازد).
        CsrfTokenRequestAttributeHandler csrfRequestHandler =
                new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .securityMatcher(
                        "/api/v1/admin/**",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh-admin",
                        "/api/v1/auth/logout-admin")
                // ⚠️ `cors` باید در هر دو زنجیره باشد، نه فقط زنجیره
                // برنامک: مسیرهای ادمین هم از مرورگر با مبدأ دیگر صدا
                // زده می‌شوند و درخواست preflight شان پیش از هر قاعده
                // `authorizeHttpRequests` می‌آید.
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        // کوکی CSRF باید از JavaScript خوانده شود تا
                        // کلاینت بتواند آن را در هدر بگذارد، پس
                        // HttpOnly نمی‌تواند باشد. خود توکن نشست ادمین
                        // اما HttpOnly است.
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        // دو استثنا:
                        // ۱) /auth/login — هنوز کوکی نشست ندارد.
                        // ۲) درخواست‌های Bearer — توکن در هدر است و
                        //    مرورگر آن را خودکار نمی‌فرستد، پس CSRF
                        //    ممکن نیست (همان استدلال زنجیره Mini App).
                        // وقتی نشست ادمین روی کوکی HttpOnly بیاید،
                        // این شاخه Bearer دیگر صدق نمی‌کند و CSRF
                        // دوباره برای آن درخواست‌ها اجباری می‌شود.
                        .ignoringRequestMatchers(request -> {
                            String uri = request.getRequestURI();
                            if ("/api/v1/auth/login".equals(uri)
                                    || "/api/v1/auth/refresh-admin".equals(uri)) {
                                return true;
                            }
                            String auth = request.getHeader("Authorization");
                            return auth != null
                                    && auth.regionMatches(true, 0, "Bearer ", 0, 7);
                        }))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh-admin").permitAll()
                        // SPA قبل از ورود هم باید توکن CSRF بگیرد
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/csrf").permitAll()
                        // ⚠️ hasAnyAuthority نه hasAnyRole: فیلتر JWT
                        // مقام‌ها را بدون پیشوند ROLE_ می‌سازد
                        // (مثل SUPER_ADMIN)، مطابق docs/security.md.
                        .anyRequest().hasAnyAuthority("SUPER_ADMIN", "ADMIN"))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * زنجیره Mini App و مسیرهای عمومی — بی‌حالت، CSRF خاموش.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ⚠️ ترتیب مهم است: مسیرهای ورود اول می‌آیند تا
                        // پیش از قاعده anyRequest بررسی شوند.
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_PATHS).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
