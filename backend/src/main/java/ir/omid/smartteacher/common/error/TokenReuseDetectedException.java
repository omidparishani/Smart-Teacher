package ir.omid.smartteacher.common.error;

import java.util.UUID;

/**
 * توکن Refresh ای ارائه شده که قبلاً استفاده (و باطل) شده است.
 *
 * <p><b>این یک رویداد امنیتی است، نه یک خطای معمولی.</b> یعنی یکی از دو
 * حالت رخ داده:
 * <ol>
 *   <li>توکن دزدیده شده و مهاجم دارد از آن استفاده می‌کند،</li>
 *   <li>یا کاربر اصلی از توکن قدیمی استفاده کرده و مهاجم زودتر آن را
 *       مصرف کرده است.</li>
 * </ol>
 * در هر دو حالت باید <b>کل خانواده توکن</b> باطل شود، نه فقط همین توکن:
 * اگر فقط همین را رد کنیم، مهاجم با نسخه‌ای که از قبل گرفته همچنان
 * دسترسی دارد. ابطال خانواده، کاربر واقعی را هم بیرون می‌کند و او مجبور
 * به ورود مجدد می‌شود — که همان رفتار درست امنیتی است.
 *
 * <p>منطق ابطال در سرویس توکن انجام می‌شود و <i>قبل</i> از پرتاب این
 * استثنا کامل می‌شود.
 */
public class TokenReuseDetectedException extends AppException {

    /** خانواده‌ای که باطل شد — فقط برای Log. */
    private final transient UUID familyId;

    public TokenReuseDetectedException(UUID familyId) {
        super(ErrorCode.TOKEN_REUSE_DETECTED, ErrorCode.TOKEN_REUSE_DETECTED.defaultMessage(), null, null);
        this.familyId = familyId;
    }

    public UUID getFamilyId() {
        return familyId;
    }
}
