/**
 * ثبت رویدادهای امنیتی — <b>رابط نام‌دار</b> ماژول {@code security}.
 *
 * <p>دلیل وجودی این فایل مثل {@code security/jwt} است: ماژول
 * {@code security} از نوع CLOSED است و زیرپکیج‌هایش از بیرون بسته‌اند،
 * ولی ورود کاربر باید بتواند رویدادهایی مثل {@code INVALID_INIT_DATA}
 * و {@code TOKEN_REUSE_DETECTED} را ثبت کند.
 *
 * <p><b>⚠️ چرا ثبت رویداد امنیتی از خودِ ماژول {@code security} انجام
 * نمی‌شود؟</b> چون آن ماژول نمی‌داند «ورود ایتا» چیست و نباید بداند؛
 * اگر می‌دانست، {@code security} به {@code eitaa} وابسته می‌شد و با
 * وابستگی موجود {@code eitaa → security} یک <b>حلقه</b> ساخته می‌شد که
 * {@code ApplicationModules.verify()} ردش می‌کند. پس جهت درست این است:
 * {@code eitaa} رویداد را می‌شناسد و ثبت می‌کند، {@code security} فقط
 * ابزار ثبت را می‌دهد.
 *
 * <p>پکیج {@code audit} سطح بالا (OPEN) جداست و {@code AuditLog} عمومی
 * را نگه می‌دارد؛ اینجا لایه <i>امنیتی</i> روی همان است.
 */
@org.springframework.modulith.NamedInterface("audit")
package ir.omid.smartteacher.security.audit;
