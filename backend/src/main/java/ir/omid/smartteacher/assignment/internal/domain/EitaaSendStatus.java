package ir.omid.smartteacher.assignment.internal.domain;

/**
 * نتیجه آخرین تلاش ارسال به ایتا هنگام انتشار.
 *
 * <p>{@code SKIPPED} یعنی اصلاً ارسال درخواست نشد (مثلاً بدون chatId).
 */
public enum EitaaSendStatus {
    SENT,
    FAILED,
    SKIPPED
}
