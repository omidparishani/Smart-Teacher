package ir.omid.smartteacher.classroom.internal.api.dto;

import ir.omid.smartteacher.classroom.internal.domain.ClassRoomStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * داشبورد یک کلاس — خلاصه‌ای که معلم با یک درخواست می‌بیند.
 *
 * <p><b>⚠️ چه چیزی عمداً اینجا نیست؟</b> شمارش تکالیف، ارسال‌ها، و
 * نمرات. آن‌ها فازهای ۷ و ۸ می‌آورند. برگرداندن صفر برایشان یعنی
 * «هیچ تکلیفی نیست» — و آن دروغ است؛ حقیقت این است که «هنوز ساخته
 * نشده». Endpoint توخالی بدتر از Endpoint باز است.
 *
 * @param classId              شناسه کلاس
 * @param name                 نام کلاس
 * @param gradeLevel           پایه؛ ممکن است {@code null} باشد
 * @param academicYear         سال تحصیلی؛ ممکن است {@code null} باشد
 * @param subjectId            شناسه درس؛ ممکن است {@code null} باشد
 * @param subjectName          نام درس؛ ممکن است {@code null} باشد
 * @param status               وضعیت کلاس
 * @param capacity             ظرفیت؛ {@code null} یعنی بدون سقف
 * @param enrolledStudentCount تعداد دانش‌آموزان فعال ثبت‌نام‌شده
 * @param seatsRemaining       صندلی خالی؛ {@code null} اگر ظرفیت تعریف نشده
 * @param createdAt            زمان ساخت کلاس
 * @param updatedAt            زمان آخرین تغییر کلاس
 */
public record ClassDashboardResponse(
        UUID classId,
        String name,
        String gradeLevel,
        String academicYear,
        UUID subjectId,
        String subjectName,
        ClassRoomStatus status,
        Integer capacity,
        long enrolledStudentCount,
        Integer seatsRemaining,
        Instant createdAt,
        Instant updatedAt
) {
}
