package ir.omid.smartteacher.student.internal.api.dto;

import ir.omid.smartteacher.student.internal.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * ورودی ساخت دانش‌آموز.
 *
 * <p><b>⚠️ چرا {@code tenantId} در ورودی نیست؟</b> چون مستأجر <i>هرگز</i>
 * از بدنه درخواست خوانده نمی‌شود. اگر می‌شد، مهاجم با عوض‌کردن یک فیلد
 * JSON می‌توانست در مستأجر دیگری بنویسد. مستأجر فقط از توکن می‌آید.
 *
 * <p><b>⚠️ چرا {@code status} در ورودی ساخت نیست؟</b> همان دلیل
 * {@code CreateClassRoomRequest}: دانش‌آموز تازه همیشه {@code ACTIVE}
 * است. تغییر وضعیت یک عمل <i>ویرایش</i> است.
 *
 * <p>⚠️ {@code nationalCode} اختیاری است ولی اگر بیاید باید رقم کنترلی
 * درست داشته باشد. تفصیل در {@code NationalCode}.
 *
 * @param fullName       نام کامل — اجباری
 * @param nationalCode   کد ملی — اختیاری، یکتا در مستأجر
 * @param guardianName   نام ولی — اختیاری
 * @param guardianMobile موبایل ولی — اختیاری، قالب ۰۹xxxxxxxxx
 * @param birthDate      تاریخ تولد — اختیاری
 * @param gender         جنسیت — اختیاری
 */
public record CreateStudentRequest(
        @NotBlank(message = "نام دانش‌آموز اجباری است.")
        @Size(max = 150, message = "نام دانش‌آموز نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String fullName,

        @Size(max = 20, message = "کد ملی نمی‌تواند بیش از ۲۰ نویسه باشد.")
        String nationalCode,

        @Size(max = 150, message = "نام ولی نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String guardianName,

        // ⚠️ قالب سخت‌گیرانه عمدی است. این فیلد «موبایل» است، نه «تلفن»؛
        // اگر ولی شماره ثابت دارد، فیلد را خالی بگذارد بهتر از این است
        // که یک شماره نامعتبر ذخیره شود و روزی پیامک به آن نرود.
        @Pattern(regexp = "^09\\d{9}$",
                message = "موبایل ولی باید ۱۱ رقم و با ۰۹ شروع شود.")
        String guardianMobile,

        @PastOrPresent(message = "تاریخ تولد نمی‌تواند در آینده باشد.")
        LocalDate birthDate,

        Gender gender
) {
}
