package ir.omid.smartteacher.classroom.internal.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * ورودی ساخت کلاس.
 *
 * <p><b>🔴 {@code homeroomTeacherId} در این ورودی <u>نیست</u> — و این
 * یک قید امنیتی است، نه یک کمبود.</b> سرویس همیشه معلم سرکلاس را برابر
 * کاربر جاری می‌گذارد. اگر این فیلد را می‌پذیرفتیم، معلمی می‌توانست
 * شناسه کاربری از مستأجر دیگر را بفرستد و آن کاربر — بدون آنکه بداند —
 * این کلاس را در فهرست خودش می‌دید، چون فهرست بر اساس همین ستون فیلتر
 * می‌شود. بررسی عضویت آن کاربر، نیازمند فهرست اعضای مستأجر است که در
 * فاز ۴ (پنل مدیریت) در دسترس می‌شود؛ تا آن روز این فیلد پذیرفته
 * نمی‌شود.
 *
 * <p>⚠️ {@code status} هم در ورودی ساخت نیست. کلاس تازه همیشه
 * {@code ACTIVE} است؛ بایگانی‌کردن یک عمل <i>ویرایش</i> است و در
 * {@link UpdateClassRoomRequest} می‌آید.
 *
 * @param name         نام کلاس — اجباری
 * @param gradeLevel   پایه تحصیلی — اختیاری
 * @param academicYear سال تحصیلی — اختیاری
 * @param subjectId    شناسه درس — اختیاری؛ اگر بیاید باید به درس
 *                     <i>همین مستأجر</i> اشاره کند
 * @param capacity     ظرفیت — اختیاری؛ سقف ۱۰۰۰ فقط برای گرفتن ورودی
 *                     بی‌معناست، نه یک محدودیت کسب‌وکار واقعی
 */
public record CreateClassRoomRequest(
        @NotBlank(message = "نام کلاس اجباری است.")
        @Size(max = 150, message = "نام کلاس نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String name,

        @Size(max = 50, message = "پایه تحصیلی نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String gradeLevel,

        @Size(max = 20, message = "سال تحصیلی نمی‌تواند بیش از ۲۰ نویسه باشد.")
        String academicYear,

        UUID subjectId,

        @Min(value = 1, message = "ظرفیت کلاس باید حداقل ۱ باشد.")
        @Max(value = 1000, message = "ظرفیت کلاس نمی‌تواند بیش از ۱۰۰۰ باشد.")
        Integer capacity
) {
}
