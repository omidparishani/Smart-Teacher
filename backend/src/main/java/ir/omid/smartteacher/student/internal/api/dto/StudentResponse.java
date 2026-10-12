package ir.omid.smartteacher.student.internal.api.dto;

import ir.omid.smartteacher.student.internal.domain.Gender;
import ir.omid.smartteacher.student.internal.domain.StudentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * نمای دانش‌آموز در پاسخ‌های API.
 *
 * <p>⚠️ {@code deletedAt} عمداً نیست: کلاینتی که فقط رکوردهای حذف‌نشده
 * را می‌بیند، به این فیلد نیازی ندارد.
 *
 * <p>⚠️ {@code guardianMobile} اینجا <b>هست</b>، برخلاف
 * {@code StudentDirectory.StudentSummary} که عمداً ندارد. تفاوت جا
 * منطقی است: این نما برای صفحه <i>پرونده دانش‌آموز</i> است و معلم
 * برای تماس با ولی به آن نیاز دارد؛ آن یکی برای فهرست کلاس است و
 * بیرون‌دادن شماره تماس در آن، سطح داده حساس را بی‌دلیل پهن می‌کند.
 *
 * @param id             شناسه دانش‌آموز
 * @param fullName       نام کامل
 * @param nationalCode   کد ملی؛ ممکن است {@code null} باشد
 * @param guardianName   نام ولی؛ ممکن است {@code null} باشد
 * @param guardianMobile موبایل ولی؛ ممکن است {@code null} باشد
 * @param birthDate      تاریخ تولد؛ ممکن است {@code null} باشد
 * @param gender         جنسیت؛ ممکن است {@code null} باشد
 * @param status         وضعیت دانش‌آموز
 * @param createdAt      زمان ساخت
 * @param updatedAt      زمان آخرین تغییر
 */
public record StudentResponse(
        UUID id,
        String fullName,
        String nationalCode,
        String guardianName,
        String guardianMobile,
        LocalDate birthDate,
        Gender gender,
        StudentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
