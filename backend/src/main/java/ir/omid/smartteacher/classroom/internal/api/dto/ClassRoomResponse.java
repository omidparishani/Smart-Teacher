package ir.omid.smartteacher.classroom.internal.api.dto;

import ir.omid.smartteacher.classroom.internal.domain.ClassRoomStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * نمای کلاس در پاسخ‌های API.
 *
 * <p><b>⚠️ {@code subjectName} اینجا هست ولی {@code homeroomTeacherName}
 * نه — و این تقارن نداشتن عمدی است.</b> نام درس از جدول همان مستأجر
 * می‌آید و بخشی از داده‌ای است که معلم در فهرست کلاس‌ها لازم دارد؛ بدون
 * آن، کلاینت باید برای هر کلاس یک درخواست جدا بفرستد. نام معلم از
 * {@code users} می‌آید که جدول سطح پلتفرم است و خواندنش در این ماژول
 * یعنی دسترسی به داده کاربری از دامنه کلاس — چیزی که فاز ۳ به آن نیاز
 * ندارد. وقتی پنل مدیریت (فاز ۴) به آن نیاز پیدا کرد، از API کاربر
 * گرفته می‌شود، نه از اینجا.
 *
 * <p>⚠️ {@code homeroomTeacherId} می‌آید چون Frontend برای تصمیم‌های
 * نمایشی (مثلاً «کلاس من» در برابر «کلاس‌های مدرسه») به آن نیاز دارد و
 * خودش یک شناسه غیرحساس است.
 *
 * @param id                شناسه کلاس
 * @param name              نام کلاس، مثل «پنجم الف»
 * @param gradeLevel        پایه تحصیلی؛ ممکن است {@code null} باشد
 * @param academicYear      سال تحصیلی؛ ممکن است {@code null} باشد
 * @param subjectId         شناسه درس؛ ممکن است {@code null} باشد
 * @param subjectName       نام درس؛ {@code null} اگر کلاس درسی ندارد.
 *                          اگر درسی که کلاس به آن اشاره می‌کرد حذف نرم
 *                          شود، این مقدار هم {@code null} می‌شود — و
 *                          همین رفتار درست است: نام درسی که دیگر وجود
 *                          ندارد نباید نمایش داده شود
 * @param homeroomTeacherId شناسه معلم سرکلاس
 * @param capacity          ظرفیت؛ ممکن است {@code null} باشد
 * @param status            وضعیت کلاس
 * @param createdAt         زمان ساخت
 * @param updatedAt         زمان آخرین تغییر
 */
public record ClassRoomResponse(
        UUID id,
        String name,
        String gradeLevel,
        String academicYear,
        UUID subjectId,
        String subjectName,
        UUID homeroomTeacherId,
        Integer capacity,
        ClassRoomStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
