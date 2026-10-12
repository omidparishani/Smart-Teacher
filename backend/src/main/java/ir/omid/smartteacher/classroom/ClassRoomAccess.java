package ir.omid.smartteacher.classroom;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * قرارداد عمومی ماژول {@code classroom} برای دسترسی به کلاس.
 *
 * <p><b>🔴 چرا این اینترفیس وجود دارد؟</b> ماژول‌هایی مثل {@code assignment}
 * باید پیش از ساخت تکلیف مطمئن شوند معلم به کلاس دسترسی دارد — همان قاعده
 * {@code ClassRoomService.requireAccessible}. بدون Facade، assignment مجبور
 * می‌شد به کلاس داخلی classroom دست بزند و {@code ApplicationModules.verify()}
 * ردش می‌کرد (قاعده ۴ {@code docs/architecture.md}).
 *
 * <p><b>⚠️ فقط خواندن و بررسی دسترسی.</b> نوشتن کلاس اینجا نیست.
 */
public interface ClassRoomAccess {

    /**
     * کلاس را می‌خواند و مالکیت معلم / مدیر را بررسی می‌کند.
     *
     * @param classroomId شناسه کلاس
     * @return خلاصه کلاس اگر کاربر حق دیدنش را داشته باشد
     * @throws ir.omid.smartteacher.common.error.ResourceNotFoundException
     *         اگر کلاس نباشد، مال مستأجر دیگر باشد، یا مال معلم دیگری
     */
    ClassRoomRef requireAccessible(UUID classroomId);

    /**
     * شناسه کلاس‌هایی که کاربر جاری حق دیدنشان را دارد.
     *
     * <p>معلم: فقط کلاس‌های خودش. مالک/مدیر مدرسه: همه کلاس‌های مستأجر.
     */
    List<UUID> listAccessibleClassroomIds();

    /**
     * آیا این دانش‌آموز در این کلاس ثبت‌نام شده است؟
     *
     * <p>⚠️ دسترسی کلاس را بررسی <b>نمی‌کند</b> — فراخوان باید اول
     * {@link #requireAccessible} را صدا بزند.
     */
    boolean isStudentEnrolled(UUID classroomId, UUID studentId);

    /**
     * نام کلاس‌های فعال با این شناسه‌ها — برای فهرست بدون N+1.
     *
     * <p>شناسه‌های ناموجود یا حذف‌شده در خروجی غایب‌اند.
     */
    Map<UUID, String> namesOf(Collection<UUID> classroomIds);

    /**
     * خلاصه عمومی یک کلاس — بدون جزئیات داخلی دامنه.
     *
     * @param id                 شناسه کلاس
     * @param name               نام
     * @param homeroomTeacherId  معلم سرکلاس؛ ممکن است {@code null} باشد
     */
    record ClassRoomRef(UUID id, String name, UUID homeroomTeacherId) {
    }
}
