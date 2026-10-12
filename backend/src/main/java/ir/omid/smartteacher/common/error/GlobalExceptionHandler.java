package ir.omid.smartteacher.common.error;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * تبدیل استثناها به پاسخ خطای استاندارد.
 *
 * <p><b>قاعده اصلی:</b> فقط {@link AppException} پیام واقعی‌اش به کاربر
 * نشان داده می‌شود. هر استثنای دیگری به یک پیام عمومی تبدیل می‌شود،
 * حتی اگر پیام فنی‌اش بی‌خطر به نظر برسد. دلیل: پیام استثناهای
 * کتابخانه‌ای می‌تواند نام جدول، ستون، مسیر فایل یا کلاس داخلی را لو
 * بدهد و نقشه‌ای از ساختار سیستم به مهاجم بدهد.
 *
 * <p>هر پاسخ خطا یک {@code traceId} دارد که <i>هم</i> به کاربر
 * برگردانده می‌شود <i>و</i> در Log ثبت می‌گردد. کاربر با همان شناسه
 * به پشتیبانی مراجعه می‌کند و ما خط مربوطه را پیدا می‌کنیم — بدون
 * اینکه لازم باشد جزئیات فنی را برای او فاش کنیم.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MDC_TRACE_ID = "traceId";

    // ─────────────────────────── خطاهای کاری ───────────────────────────

    /**
     * استثناهای دامنه — تنها دسته‌ای که پیامش به کاربر می‌رسد.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex,
                                                       HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        String traceId = traceId();

        if (code.httpStatus() >= 500) {
            // خطای سمت سرور: جزئیات فنی برای ما مهم است، برای کاربر نه.
            LOG.error("traceId={} خطای کاری {} در {} — {}",
                    traceId, code, request.getRequestURI(), internalDetail(ex), ex);
        } else {
            LOG.warn("traceId={} خطای کاری {} در {} — {}",
                    traceId, code, request.getRequestURI(), ex.getMessage());
        }

        // ⚠️ برای خطاهای ۵xx پیام پیش‌فرض می‌فرستیم، نه پیام استثنا.
        // حتی پیام خودمان هم نباید برای خطای داخلی به بیرون برود.
        String message = (code.httpStatus() >= 500)
                ? code.defaultMessage()
                : ex.getMessage();

        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, message, traceId, request.getRequestURI(), ex.getDetails()));
    }

    // ─────────────────────────── اعتبارسنجی ───────────────────────────

    /**
     * خطای {@code @Valid} روی بدنه درخواست.
     *
     * <p><b>⚠️ مقدار ردشده برگردانده نمی‌شود.</b> پاسخ خطا در مرورگر و
     * Log پروکسی‌ها ثبت می‌شود؛ اگر مقدار فیلد را هم بفرستیم، رمزی که
     * کاربر اشتباه تایپ کرده در چند جای دیگر باقی می‌ماند.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ApiError.FieldViolation.of(
                        error.getField(),
                        // پیام پیش‌فرض Bean Validation انگلیسی است؛ اگر
                        // پیام اختصاصی فارسی تنظیم شده باشد همان می‌آید.
                        error.getDefaultMessage()))
                .toList();

        return validationResponse(violations, request);
    }

    /** خطای اعتبارسنجی روی پارامتر مسیر یا پارامترهای پرس‌وجو. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getConstraintViolations()
                .stream()
                .map(violation -> ApiError.FieldViolation.of(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();

        return validationResponse(violations, request);
    }

    private static ResponseEntity<ApiError> validationResponse(
            List<ApiError.FieldViolation> violations, HttpServletRequest request) {
        String traceId = traceId();
        LOG.warn("traceId={} خطای اعتبارسنجی در {} — {} مورد",
                traceId, request.getRequestURI(), violations.size());

        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
                .body(ApiError.ofValidation(violations, traceId, request.getRequestURI()));
    }

    // ─────────────────────────── درخواست بد ───────────────────────────

    /** بدنه درخواست قابل خواندن نیست — JSON خراب یا نوع نادرست. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest request) {
        String traceId = traceId();
        LOG.warn("traceId={} بدنه درخواست خوانده نشد در {}",
                traceId, request.getRequestURI());

        ErrorCode code = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, code.defaultMessage(), traceId,
                        request.getRequestURI()));
    }

    /**
     * مقدار پارامتر مسیر یا پرس‌وجو به نوع مورد انتظار تبدیل نشد.
     *
     * <p><b>⚠️ چرا این هندلر لازم است؟</b> بدون آن، درخواست
     * {@code GET /classes/abc} یا {@code ?status=FOO} به تور اطمینان
     * می‌افتاد و کاربر {@code 500} می‌گرفت. این خطای <i>کلاینت</i> است،
     * نه خطای سرور: هیچ‌چیز در سرور خراب نشده، فقط ورودی جور نبوده.
     * پاسخ درست {@code 400} است، وگرنه پایش سرور با هشدارهای دروغ پر
     * می‌شود و خطاهای واقعی در میانشان گم می‌شوند.
     *
     * <p>⚠️ مقدار ردشده در پاسخ برنمی‌گردد — همان قاعده‌ای که در
     * {@link #handleValidation} توضیح داده شده. فقط <i>نام</i> پارامتر
     * می‌آید که از کد خودمان است، نه از ورودی کاربر.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String traceId = traceId();
        String parameterName = ex.getName();

        LOG.warn("traceId={} مقدار نامعتبر برای پارامتر {} در {}",
                traceId, parameterName, request.getRequestURI());

        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
                .body(ApiError.ofValidation(
                        List.of(ApiError.FieldViolation.of(parameterName,
                                "مقدار این پارامتر نامعتبر است.")),
                        traceId,
                        request.getRequestURI()));
    }

    /** متد HTTP پشتیبانی نمی‌شود. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.INVALID_REQUEST;
        String traceId = traceId();
        LOG.warn("traceId={} متد {} پشتیبانی نمی‌شود در {}",
                traceId, ex.getMethod(), request.getRequestURI());

        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, code.defaultMessage(), traceId,
                        request.getRequestURI()));
    }

    /** مسیری وجود ندارد. */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoHandlerFoundException ex,
                                                   HttpServletRequest request) {
        ErrorCode code = ErrorCode.ENDPOINT_NOT_FOUND;
        String traceId = traceId();
        LOG.warn("traceId={} مسیر یافت نشد: {}", traceId, request.getRequestURI());

        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, code.defaultMessage(), traceId,
                        request.getRequestURI()));
    }

    // ─────────────────────────── دسترسی ───────────────────────────

    /**
     * رد شدن {@code @PreAuthorize}.
     *
     * <p>عمداً {@code 403} است و نه {@code 404}: اینجا کاربر درخواست
     * عملیاتی را داده که مجوزش را ندارد و <i>هیچ رکوردی</i> در میان
     * نیست که وجودش لو برود. تفاوتش با {@code TenantGuard} مهم است —
     * آنجا رکورد مشخصی هدف بود و باید {@code 404} بدهیم.
     *
     * <p><b>⚠️ چرا اینجا {@code TenantContext} خوانده نمی‌شود؟</b> چون
     * {@code common} نباید به ماژول {@code tenant} وابسته شود —
     * {@code tenant} خودش به {@code common} وابسته است و این وابستگی
     * دوطرفه، قید معماری Spring Modulith را می‌شکند و
     * {@code ApplicationModules.verify()} را رد می‌کند. هویت کاربر در
     * {@code SecurityAuditLog} ثبت می‌شود که جای درستش همان‌جاست.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        ErrorCode code = ErrorCode.ACCESS_DENIED;
        String traceId = traceId();
        LOG.warn("traceId={} دسترسی رد شد در {}", traceId, request.getRequestURI());

        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, code.defaultMessage(), traceId,
                        request.getRequestURI()));
    }

    // ─────────────────────────── تعارض داده ───────────────────────────

    /**
     * تور اطمینان برای تعارض‌های سطح دیتابیس.
     *
     * <p><b>⚠️ چرا این هندلر لازم است؟</b> بدون آن، نقض یک قید یکتایی
     * به {@link Exception} عمومی می‌افتاد و کاربر {@code 500} می‌گرفت —
     * یعنی «سرور خراب است» برای چیزی که در واقع «این نام قبلاً ثبت
     * شده» است.
     *
     * <p><b>⚠️ چرا پیام «قبلاً ثبت شده» نمی‌دهیم؟</b> چون این استثنا
     * چند علت مختلف دارد: نقض یکتایی، کلید خارجی نامعتبر، مقدار
     * {@code NOT NULL} خالی. اگر پیام یکتایی بدهیم، برای بقیه دروغ
     * می‌گوید و کاربر را دنبال علت اشتباه می‌فرستد. سرویس‌ها تکرار را
     * <i>پیش از</i> نوشتن بررسی می‌کنند؛ این هندلر فقط مسابقه همزمانی
     * و باگ را می‌گیرد و پیام صادقانه‌اش «تعارض با وضعیت فعلی» است.
     *
     * <p>جزئیات واقعی (نام قید، نام جدول) فقط در Log می‌ماند — همان
     * قاعده‌ای که نمی‌گذارد ساختار دیتابیس به مهاجم نقشه بدهد.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        ErrorCode code = ErrorCode.RESOURCE_CONFLICT;
        String traceId = traceId();
        LOG.warn("traceId={} تعارض داده در {} — {}",
                traceId, request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, code.defaultMessage(), traceId,
                        request.getRequestURI()));
    }

    /**
     * دو درخواست همزمان یک رکورد را تغییر داده‌اند.
     *
     * <p>{@code @Version} روی {@code BaseEntity} این را تشخیص می‌دهد.
     * بدون این هندلر، آخرین نویسنده بی‌صدا برنده نمی‌شد ولی کاربر هم
     * چیزی جز {@code 500} نمی‌دید.
     *
     * <p>پیام عمداً می‌گوید «دوباره تلاش کنید»، چون این خطا گذراست:
     * بار دوم که کلاینت نسخه تازه را می‌خواند، نوشتن موفق می‌شود.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.RESOURCE_CONFLICT;
        String traceId = traceId();
        LOG.warn("traceId={} تعارض نسخه در {}", traceId, request.getRequestURI());

        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code,
                        "این مورد همزمان توسط درخواست دیگری تغییر کرده است. دوباره تلاش کنید.",
                        traceId, request.getRequestURI()));
    }

    // ─────────────────────────── تور اطمینان ───────────────────────────

    /**
     * هر استثنای دیگری.
     *
     * <p>پیام و ردپای کامل فقط در Log می‌ماند. کاربر فقط یک شناسه
     * ردیابی می‌گیرد که با آن می‌توانیم خط مربوطه را پیدا کنیم.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex,
                                                     HttpServletRequest request) {
        String traceId = traceId();

        // ⚠️ اینجا است که ردپای کامل ثبت می‌شود — تنها جایی که مجاز است.
        LOG.error("traceId={} خطای پیش‌بینی‌نشده در {}",
                traceId, request.getRequestURI(), ex);

        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, code.defaultMessage(), traceId,
                        request.getRequestURI()));
    }

    // ─────────────────────────── کمکی ───────────────────────────

    /**
     * شناسه ردیابی برای همبستگی پاسخ و Log.
     *
     * <p>اگر لایه ردیابی (Micrometer Tracing) فعال باشد، همان
     * {@code traceId} واقعی از MDC خوانده می‌شود تا این خط با بقیه خطوط
     * همان درخواست گره بخورد. اگر نباشد، شناسه‌ای ساخته می‌شود تا کاربر
     * باز هم چیزی برای ارجاع داشته باشد.
     *
     * <p>عمداً در MDC <i>نوشته نمی‌شود</i>: نوشتن بدون پاک‌سازی در
     * Thread‌های بازاستفاده‌شده، شناسه یک درخواست را به درخواست بعدی
     * نشت می‌دهد.
     */
    private static String traceId() {
        String existing = MDC.get(MDC_TRACE_ID);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return UuidV7Generator.generate().toString();
    }

    /**
     * استخراج دلیل فنی برای Log.
     *
     * <p>بعضی استثناهای ما دلیل واقعی را در فیلد جدا نگه می‌دارند (تا
     * به کاربر نشت نکند) و در {@code getMessage()} پیام عمومی دارند.
     * بدون این متد، دلیل واقعی هرگز در Log نمی‌افتاد و عیب‌یابی عملاً
     * غیرممکن می‌شد.
     */
    private static String internalDetail(AppException ex) {
        if (ex instanceof EncryptionException encryptionException
                && encryptionException.getInternalReason() != null) {
            return encryptionException.getInternalReason();
        }
        if (ex instanceof InvalidTokenException invalidTokenException
                && invalidTokenException.getInternalReason() != null) {
            return invalidTokenException.getInternalReason();
        }
        if (ex instanceof TenantNotResolvedException tenantNotResolved) {
            return tenantNotResolved.getOperation();
        }
        return ex.getMessage();
    }
}
