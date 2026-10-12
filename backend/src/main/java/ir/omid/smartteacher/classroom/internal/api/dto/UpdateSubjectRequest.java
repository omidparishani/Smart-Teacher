package ir.omid.smartteacher.classroom.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ورودی ویرایش درس — جایگزینی کامل ({@code PUT}).
 *
 * <p>شکلش عیناً {@link CreateSubjectRequest} است ولی record جدا مانده و
 * عمدی است: اگر یکی شوند، روزی که ویرایش فیلد تازه‌ای بگیرد (مثلاً
 * «وضعیت») همان فیلد بی‌سروصدا به ورودی <i>ساخت</i> هم اضافه می‌شود.
 * دو قرارداد جدا، دو record جدا — حتی وقتی امروز یکی‌اند.
 *
 * @param name       نام درس — اجباری
 * @param code       کد درس — اختیاری
 * @param gradeLevel پایه تحصیلی — اختیاری
 */
public record UpdateSubjectRequest(
        @NotBlank(message = "نام درس اجباری است.")
        @Size(max = 150, message = "نام درس نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String name,

        @Size(max = 50, message = "کد درس نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String code,

        @Size(max = 50, message = "پایه تحصیلی نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String gradeLevel
) {
}
