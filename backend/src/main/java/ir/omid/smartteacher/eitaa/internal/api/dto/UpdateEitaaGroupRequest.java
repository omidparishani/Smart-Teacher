package ir.omid.smartteacher.eitaa.internal.api.dto;

/**
 * ویرایش گروه ایتا — به‌روزرسانی <b>جزئی</b>.
 *
 * <p>قرارداد فیلدها (سه حالت، نه دو حالت):
 *
 * <ul>
 *   <li>{@code eitaaChatId} — {@code null} یا رشتهٔ خالی یعنی «تغییر نده».
 *       این شناسهٔ اصلی مقصد است و پاک‌کردنش معنا ندارد؛ گروهی بدون
 *       {@code chat_id} مقصد ارسال نیست.</li>
 *   <li>{@code title} — {@code null} یعنی «تغییر نده»؛ رشتهٔ خالی یعنی
 *       «پاک کن» (در سرویس با {@code trimToNull} به {@code null} می‌رود).
 *       تفاوت {@code null} و {@code ""} اینجا معنادار است، پس با
 *       {@code @NotBlank} نمی‌شود بیانش کرد.</li>
 *   <li>{@code type} — {@code null} <b>یا رشتهٔ خالی</b> یعنی «تغییر نده».
 *       ⚠️ این عمداً با {@code title} فرق دارد: ستون {@code type} در پایگاه
 *       داده {@code NOT NULL} است، پس «پاک‌کردن» برایش معنا ندارد و
 *       {@code null} کردنش استثنای پایگاه داده می‌دهد.</li>
 * </ul>
 *
 * <p>⚠️ فیلدها اعتبارسنجی Bean ندارند چون «{@code null} یعنی تغییر نده»
 * با annotationهای استاندارد قابل بیان نیست. اعتبارسنجی واقعی
 * ({@code chat_id}) در سرویس انجام می‌شود.
 */
public record UpdateEitaaGroupRequest(
        String eitaaChatId,
        String title,
        String type
) {}
