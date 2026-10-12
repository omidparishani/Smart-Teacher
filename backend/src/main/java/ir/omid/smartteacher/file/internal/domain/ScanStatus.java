package ir.omid.smartteacher.file.internal.domain;

/**
 * وضعیت پویش بدافزار — در گام ۱ همیشه {@link #PENDING} می‌ماند؛
 * پویش واقعی در فازهای بعدی.
 */
public enum ScanStatus {
    PENDING,
    CLEAN,
    INFECTED,
    FAILED
}
