package ir.omid.smartteacher.ai;

/**
 * درگاه تحلیل محتوا برای ماژول کتاب — بدون افشای Repositoryهای AI.
 *
 * <p>کتاب فقط این پورت را صدا می‌زند؛ جزئیات {@link AiProvider} و ثبت
 * مصرف داخل ماژول AI می‌ماند.
 */
public interface ContentAnalysisAi {

    /**
     * تولید متن تحلیل برای یک صفحه/فصل/کتاب.
     *
     * @param analysisType نوع تحلیل: SUMMARY|QUESTIONS|TEACHING_PLAN|DIFFICULTY
     * @param sourceText   متن استخراج‌شده یا جایگزین OCR
     * @return متن نتیجه؛ هرگز {@code null}
     */
    String analyze(String analysisType, String sourceText);

    /**
     * نمره ساختاریافته برای یک ارسال تکلیف.
     *
     * @param assignmentTitle عنوان تکلیف
     * @param answerText      پاسخ دانش‌آموز
     * @param maxScore        سقف نمره؛ می‌تواند {@code null} باشد
     * @return نمره ساختگی/واقعی ساختاریافته
     */
    SubmissionGrade gradeSubmission(String assignmentTitle,
                                    String answerText,
                                    java.math.BigDecimal maxScore);

    /**
     * خروجی Structured تصحیح تکلیف.
     *
     * @param score       نمره پیشنهادی
     * @param correct     آیا پاسخ درست است
     * @param mistakes    اشتباهات
     * @param strengths   نقاط قوت
     * @param weaknesses  نقاط ضعف
     * @param feedback    بازخورد برای دانش‌آموز
     * @param teacherNote یادداشت برای معلم
     * @param confidence  اطمینان ۰..۱
     */
    record SubmissionGrade(
            java.math.BigDecimal score,
            boolean correct,
            java.util.List<String> mistakes,
            java.util.List<String> strengths,
            java.util.List<String> weaknesses,
            String feedback,
            String teacherNote,
            java.math.BigDecimal confidence
    ) {
    }
}
