package ir.omid.smartteacher.classroom.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * نمای یک دانش‌آموز ثبت‌نام‌شده در کلاس.
 *
 * <p>⚠️ {@code guardianMobile} عمداً نیست — این نما برای <i>فهرست کلاس</i>
 * است و شماره تماس ولی داده حساس است. برای تماس، معلم به
 * {@code GET /students/{id}} می‌رود.
 *
 * @param studentId    شناسه دانش‌آموز
 * @param fullName     نام کامل
 * @param nationalCode کد ملی؛ ممکن است {@code null} باشد
 * @param rollNumber   شماره حضور؛ ممکن است {@code null} باشد
 * @param enrolledAt   زمان ثبت‌نام
 */
public record ClassStudentResponse(
        UUID studentId,
        String fullName,
        String nationalCode,
        String rollNumber,
        Instant enrolledAt
) {
}
