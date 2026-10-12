package ir.omid.smartteacher.assignment.internal.api.dto;

import ir.omid.smartteacher.assignment.internal.domain.SubmissionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * پاسخ یک ارسال.
 *
 * @param id                 شناسه ارسال
 * @param assignmentId       تکلیف
 * @param studentId          دانش‌آموز
 * @param studentName        نام (اگر در دسترس باشد)
 * @param answerText         متن پاسخ
 * @param status             وضعیت
 * @param submittedAt        زمان ارسال
 * @param submittedByUserId  کاربری که ثبت کرده
 * @param finalScore         نمره نهایی
 * @param teacherOverrodeAi  آیا معلم نمره AI را عوض کرده
 */
public record SubmissionResponse(
        UUID id,
        UUID assignmentId,
        UUID studentId,
        String studentName,
        String answerText,
        SubmissionStatus status,
        Instant submittedAt,
        UUID submittedByUserId,
        BigDecimal finalScore,
        boolean teacherOverrodeAi
) {
}
