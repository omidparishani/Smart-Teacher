package ir.omid.smartteacher.eitaa.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.eitaa.EitaaProperties;
import ir.omid.smartteacher.eitaa.internal.api.dto.AuthResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.CurrentUserResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaLoginRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.RefreshTokenRequest;
import ir.omid.smartteacher.eitaa.internal.application.EitaaAuthService;
import ir.omid.smartteacher.security.jwt.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مسیرهای احراز هویت برنامک — {@code docs/api.md} بخش ۵.
 *
 * <p>چهار مسیر: ورود با {@code initData}، پروفایل، تازه‌سازی، خروج.
 *
 * <p><b>⚠️ این کلاس هیچ منطقی ندارد و نباید داشته باشد.</b> تنها کارش
 * تبدیل HTTP به فراخوان سرویس است. اگر روزی اعتبارسنجی یا شرطی اینجا
 * اضافه شود، همان قاعده‌ای نقض شده که تست‌های سرویس را ممکن می‌کند:
 * منطق باید بدون راه‌اندازی وب هم قابل آزمون باشد.
 *
 * <p><b>🔴 چرا در ماژول {@code eitaa}؟</b> چون
 * {@code docs/architecture.md} بخش ۴، «Mini App Auth» را جزو تعریف ماژول
 * {@code eitaa} آورده و ماژول {@code auth} در فهرست ۳۲ ماژول مصوب وجود
 * ندارد. مسیر URL ({@code /api/v1/auth/...}) ربطی به محل کد ندارد.
 *
 * <p><b>⚠️ محدودیت شناخته‌شده — محدودیت نرخ:</b> این مسیرها هیچ سقف
 * درخواستی ندارند. یک مهاجم می‌تواند مسیر ورود را با {@code initData}
 * جعلی بمباران کند؛ هر تلاش یک HMAC و یک کوئری دیتابیس هزینه دارد.
 * محدودکننده نرخ در فاز ۱۲ می‌آید ({@code docs/roadmap.md}) و برای
 * {@code Redis} طراحی شده که هنوز در پشته نیست. این مورد در
 * {@code docs/verification-log.md} ثبت شده است — تنها سپر فعلی،
 * پنجره تازگی و نگهبان بازپخش است.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "احراز هویت", description = "ورود برنامک ایتا و مدیریت نشست")
public class EitaaAuthController {

    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_USER_AGENT = "User-Agent";

    private final EitaaAuthService authService;
    private final EitaaProperties properties;

    public EitaaAuthController(EitaaAuthService authService, EitaaProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    // ─────────────────────────── ورود ───────────────────────────

    /**
     * ورود با {@code initData} برنامک ایتا.
     *
     * <p>اگر کاربر تازه باشد، حساب و مستأجر شخصی‌اش خودکار ساخته
     * می‌شود — بدون هیچ فرمی.
     *
     * <p><b>⚠️ چرا ۲۰۰ و نه ۲۰۱ برای کاربر تازه؟</b> چون منبعی ساخته
     * نمی‌شود که کلاینت با آن کار کند؛ پاسخ توکن است. وضعیت ۲۰۱ کلاینت
     * را وادار می‌کند هدر {@code Location} را دنبال کند که اینجا وجود
     * ندارد. تازه‌بودن حساب در فیلد {@code isNewUser} پاسخ می‌آید.
     */
    @PostMapping("/eitaa")
    @Operation(summary = "ورود با initData ایتا",
            description = "initData برنامک را اعتبارسنجی می‌کند و جفت توکن برمی‌گرداند. "
                    + "در نخستین ورود، حساب کاربر و مستأجر شخصی‌اش ساخته می‌شود.")
    public AuthResponse login(@Valid @RequestBody EitaaLoginRequest request,
                              HttpServletRequest httpRequest) {
        return authService.login(request.initData(), clientInfoOf(httpRequest));
    }

    // ─────────────────────────── پروفایل ───────────────────────────

    /**
     * پروفایل کاربر واردشده.
     *
     * <p>نیازمند {@code Authorization: Bearer} است — مسیر عمومی نیست.
     */
    @GetMapping("/me")
    @Operation(summary = "پروفایل کاربر جاری",
            description = "کاربر، مستأجر فعال و اشتراک. توکنی برنمی‌گرداند.")
    public CurrentUserResponse me() {
        return authService.currentUser();
    }

    // ─────────────────────────── تازه‌سازی ───────────────────────────

    /**
     * تازه‌سازی جفت توکن.
     *
     * <p>نیازمند توکن دسترسی نیست — توکن تازه‌سازی خودش مدرک است. علت:
     * اگر توکن دسترسی منقضی شده باشد، کلاینت باید بتواند بدون آن
     * تازه‌سازی کند؛ وگرنه تازه‌سازی بی‌فایده می‌شد.
     *
     * <p>⚠️ پاسخ برای هر دو حالت ورود و تازه‌سازی {@link AuthResponse}
     * است، پس کلاینت فقط یک شکل را می‌شناسد. تفاوت در
     * {@code isNewUser} است که اینجا همیشه {@code false} می‌آید.
     */
    @PostMapping("/refresh")
    @Operation(summary = "تازه‌سازی توکن‌ها",
            description = "توکن تازه‌سازی را می‌چرخاند. استفاده مجدد از توکن مصرف‌شده "
                    + "کل خانواده توکن را باطل می‌کند.")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request,
                                HttpServletRequest httpRequest) {
        return authService.refresh(request.refreshToken(), clientInfoOf(httpRequest));
    }

    // ─────────────────────────── خروج ───────────────────────────

    /**
     * خروج — باطل‌کردن کل خانواده توکن.
     *
     * <p><b>⚠️ همیشه ۲۰۴، حتی با توکن ناشناخته یا نامعتبر.</b> خروج
     * عملیاتی است که کاربر <i>قصد انجامش</i> را دارد؛ شکست دادنش هیچ
     * چیزی محافظت نمی‌کند و فقط باعث می‌شود کلاینت توکن را پاک نکند.
     * جزئیات دلیل در {@code AuthTokenService.revokeFamily} آمده.
     *
     * <p>⚠️ مسیر عمومی نیست و توکن دسترسی می‌خواهد؛ اما این یک سخت‌گیری
     * بی‌معنا نیست، چون توکن تازه‌سازی هم در بدنه می‌آید و کلاینتی که
     * توکن دسترسی‌اش منقضی شده می‌تواند ابتدا تازه‌سازی کند. سختی کار
     * برای کلاینت در برابر کوتاه‌شدن پنجره حمله، بهای درستی است.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "خروج",
            description = "کل خانواده توکن تازه‌سازی را باطل می‌کند. پاسخ همیشه ۲۰۴ است.")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }

    // ─────────────────────────── کمکی ───────────────────────────

    /**
     * اطلاعات کلاینت برای ثبت روی رکورد توکن.
     *
     * <p><b>⚠️ درباره IP:</b> پیش‌فرض {@code getRemoteAddr()} است و نه
     * هدر {@code X-Forwarded-For} — همان تصمیمی که
     * {@code SecurityAuditLog} گرفته و دلیلش یکی است: آن هدر را کلاینت
     * می‌تواند جعل کند. اگر پشت پروکسی معتمد هستید،
     * {@code app.eitaa.trust-forwarded-for=true} را روشن کنید؛ آن
     * وقت تنها باید <i>اولین</i> مقدار فهرست خوانده شود، چون
     * پروکسی‌های بعدی مقدار خودشان را به همان هدر اضافه می‌کنند.
     *
     * <p>⚠️ اگر کلاینت هدر {@code User-Agent} ندهد، {@code null} ذخیره
     * می‌شود — و این مجاز است. ساختن مقداری مثل {@code "unknown"}
     * یعنی در دیتابیس نمی‌توان «ندارد» را از «مقدارش unknown بود»
     * تشخیص داد.
     */
    private AuthTokenService.ClientInfo clientInfoOf(HttpServletRequest request) {
        return new AuthTokenService.ClientInfo(
                clientIp(request),
                request.getHeader(HEADER_USER_AGENT));
    }

    private String clientIp(HttpServletRequest request) {
        if (properties.trustForwardedFor()) {
            String forwarded = request.getHeader(HEADER_FORWARDED_FOR);
            String first = firstEntry(forwarded);
            if (first != null) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    /** اولین مقدار یک هدر فهرستی — {@code "client, proxy1, proxy2"} ← {@code "client"}. */
    private static String firstEntry(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        int comma = headerValue.indexOf(',');
        String first = (comma < 0) ? headerValue : headerValue.substring(0, comma);
        String trimmed = first.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
