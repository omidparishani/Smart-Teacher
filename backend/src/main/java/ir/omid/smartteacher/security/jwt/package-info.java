/**
 * صدور و اعتبارسنجی توکن — <b>رابط نام‌دار</b> ماژول {@code security}.
 *
 * <p><b>چرا این فایل لازم است؟</b> ماژول {@code security} از نوع CLOSED
 * است، یعنی از بیرون فقط پکیج ریشه‌اش دیده می‌شود و زیرپکیج‌ها بسته‌اند.
 * ماژول‌های {@code eitaa} و {@code admin} برای ورود به {@code JwtService}،
 * {@code AuthTokenService}، {@code TokenPair} و مخزن توکن نیاز دارند.
 * سه راه بود:
 *
 * <ol>
 *   <li>این کلاس‌ها به پکیج ریشه {@code security} منتقل شوند — یعنی
 *       همه ماژول‌ها به جزئیات JWT دسترسی پیدا کنند، در حالی که فقط
 *       ورود و تازه‌سازی لازمشان است.</li>
 *   <li>ماژول {@code security} از CLOSED به OPEN تغییر کند — یعنی
 *       همه مرزها یک‌جا برداشته شوند.</li>
 *   <li>همین پکیج «رابط نام‌دار» اعلام شود.</li>
 * </ol>
 *
 * <p>راه سوم انتخاب شد چون دقیق‌ترین است: فقط همین یک پکیج باز می‌شود،
 * با نامی که در {@code allowedDependencies} قابل ارجاع است
 * ({@code security::jwt}). بقیه {@code security} — رمزنگاری،
 * {@code TenantGuard}، پیکربندی فیلترها — بسته می‌ماند.
 *
 * <p><b>⚠️ چرا {@code @ApplicationModule} روی خودِ {@code security}
 * عوض نشد؟</b> چون سطح دسترسی را باید جایی باز کرد که واقعاً لازم است،
 * نه جایی که راحت است.
 */
@org.springframework.modulith.NamedInterface("jwt")
package ir.omid.smartteacher.security.jwt;
