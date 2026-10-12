/**
 * ماژول ایتا — تنها دروازه گفت‌وگو با ایتایار و اعتبارسنجی {@code initData}.
 *
 * <p>این ماژول {@code CLOSED} است: فقط کلاس‌های <b>ریشه همین پکیج</b>
 * برای ماژول‌های دیگر قابل دسترسی‌اند. پیاده‌سازی‌ها در
 * {@code ir.omid.smartteacher.eitaa.internal} پنهان‌اند تا ماژول‌های
 * دیگر به HTTP Client یا جزئیات قالب درخواست ایتایار گره نخورند.
 *
 * <p><b>🔴 قید حاکم بر این ماژول:</b> ایتا هیچ مسیر دریافت پیام ندارد
 * ({@code docs/eitaa.md} بخش ۶). نه Webhook، نه Long-Polling، نه
 * {@code getUpdates}. پس هیچ کلاسی اینجا برای «دریافت» وجود ندارد و
 * نباید اضافه شود. {@code ArchUnit_noWebhookControllerTest} همین را
 * اجبار می‌کند.
 *
 * <p>هر قابلیتی که در {@link ir.omid.smartteacher.eitaa.EitaaCapability}
 * نیست، وجود ندارد — و در کد هم شبیه‌سازی نمی‌شود.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Eitaa")
package ir.omid.smartteacher.eitaa;
