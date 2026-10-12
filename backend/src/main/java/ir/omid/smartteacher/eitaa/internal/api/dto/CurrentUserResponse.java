package ir.omid.smartteacher.eitaa.internal.api.dto;

/**
 * پاسخ {@code GET /auth/me} — پروفایل کاربر واردشده.
 *
 * <p><b>⚠️ چرا توکن ندارد؟</b> چون این مسیر با یک توکن معتبر صدا زده
 * می‌شود؛ برگرداندن توکن تازه اینجا یعنی هر بار صدا زدن پروفایل، یک
 * توکن جدید صادر شود — بی‌دلیل و پرخطر. توکن فقط از مسیر
 * {@code /auth/eitaa} و {@code /auth/refresh} می‌آید.
 *
 * <p>ترکیب سه نمای زیر، همان چیزی است که ورود برمی‌گرداند، منهای
 * توکن‌ها. یکسان نگه‌داشتنشان عمدی است: کلاینت یک شکل را می‌شناسد.
 *
 * @param user         کاربر
 * @param tenant       مستأجر فعال — می‌تواند {@code null} باشد
 * @param subscription اشتراک — در فاز ۲ همیشه {@code null}
 */
public record CurrentUserResponse(
        AuthUserView user,
        AuthTenantView tenant,
        AuthSubscriptionView subscription
) {
}
