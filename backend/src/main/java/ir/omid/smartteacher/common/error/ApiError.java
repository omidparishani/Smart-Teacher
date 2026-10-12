package ir.omid.smartteacher.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * بدنه استاندارد پاسخ خطا ({@code docs/api.md} بخش ۳).
 *
 * @param timestamp زمان وقوع — ISO-8601 با منطقه زمانی
 * @param status    کد وضعیت HTTP
 * @param code      کد خطای ماشین‌خوان
 * @param message   پیام فارسی کاربرپسند
 * @param traceId   شناسه ردیابی — کاربر با این مقدار به پشتیبانی مراجعه می‌کند
 * @param path      مسیر درخواست
 * @param details   جزئیات تکمیلی؛ برای خطای اعتبارسنجی فهرست
 *                  {@link FieldViolation}، برای خطای سقف مصرف یک شیء
 *                  اطلاعاتی. نوعش {@code Object} است چون هر خطا شکل
 *                  جزئیات متفاوتی دارد.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        String traceId,
        String path,
        Object details
) {

    /**
     * یک تخلف اعتبارسنجی روی یک فیلد مشخص.
     *
     * <p><b>⚠️ هشدار:</b> {@code rejectedValue} ممکن است داده حساس باشد.
     * برای فیلدهای رمز، توکن و کلید، سرویس فراخوان باید مقدار را
     * {@code null} بگذارد.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldViolation(String field, String message, Object rejectedValue) {

        public static FieldViolation of(String field, String message) {
            return new FieldViolation(field, message, null);
        }
    }

    public static ApiError of(ErrorCode code, String message, String traceId, String path) {
        return new ApiError(Instant.now(), code.httpStatus(), code, message, traceId, path, null);
    }

    public static ApiError of(ErrorCode code,
                              String message,
                              String traceId,
                              String path,
                              Object details) {
        return new ApiError(Instant.now(), code.httpStatus(), code, message, traceId, path, details);
    }

    public static ApiError ofValidation(List<FieldViolation> violations,
                                        String traceId,
                                        String path) {
        return new ApiError(
                Instant.now(),
                ErrorCode.VALIDATION_FAILED.httpStatus(),
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultMessage(),
                traceId,
                path,
                violations);
    }
}
