package ir.omid.smartteacher.tenant;

import ir.omid.smartteacher.common.error.TenantNotResolvedException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * زمینه مستأجر جاری برای هر درخواست.
 *
 * <p>پیاده‌سازی بر پایه {@link ScopedValue} است (در Java 25 نهایی شده)
 * و نه {@code ThreadLocal}. تفاوت عملی مهم است: با Virtual Threadها هر
 * درخواست ممکن است روی چند Thread جابه‌جا شود و {@code ThreadLocal}
 * نیازمند پاک‌سازی دستی است؛ اگر یک بار فراموش شود، درخواست بعدی روی
 * همان Thread داده‌ی مستأجر قبلی را می‌بیند — یک نشت داده‌ی بین‌مستأجری.
 * {@code ScopedValue} در پایان بلوک خودبه‌خود بازمی‌گردد و چنین نشتی
 * ممکن نیست.
 *
 * <p><b>⚠️ محدودیت مهم:</b> {@code ScopedValue} به Threadهای دیگر منتقل
 * نمی‌شود. هر کدی که روی Thread جدا اجرا شود — {@code @Async}،
 * {@code ExecutorService}، Job زمان‌بندی‌شده — زمینه مستأجر را
 * <b>از دست می‌دهد</b> و {@link #requireTenantId()} در آن پرتاب
 * می‌کند. این رفتار عمدی و Fail-Closed است: ادامه‌دادن بدون مستأجر به
 * کوئری بدون فیلتر و نشت داده منجر می‌شود. آن کدها باید صریحاً با
 * {@link #callAs} زمینه را دوباره برقرار کنند.
 *
 * <p>الگوی استفاده در فیلتر:
 * <pre>{@code
 * TenantContext.runAsChecked(principal, () -> chain.doFilter(request, response));
 * }</pre>
 */
public final class TenantContext {

    /**
     * شناسه جایگزین برای دامنه پلتفرم.
     *
     * <p><b>چرا صفر و نه {@code null}؟</b> چون Hibernate از
     * {@code CurrentTenantIdentifierResolver} یک مقدار غیر-null
     * می‌خواهد و در بازکردن Session صدایش می‌زند — حتی برای مسیرهای
     * عمومی مثل ورود که هنوز مستأجری وجود ندارد. اگر آنجا استثنا
     * پرتاب کنیم، ورود کاربر غیرممکن می‌شود. پس دامنه پلتفرم را با
     * صفر نشان می‌دهیم؛ چون هیچ رکوردی با {@code tenant_id = 0} وجود
     * ندارد، هر کوئری روی جدول‌های مستأجری در این دامنه <b>خالی</b>
     * برمی‌گردد — یعنی همان Fail-Closed، بدون شکستن ورود.
     */
    public static final UUID PLATFORM_SENTINEL = new UUID(0L, 0L);

    private static final ScopedValue<TenantPrincipal> PRINCIPAL = ScopedValue.newInstance();

    private TenantContext() {
        throw new AssertionError("کلاس زمینه نباید نمونه‌سازی شود");
    }

    // ─────────────────────────── اجرا در محدوده ───────────────────────────

    /**
     * اجرای یک عملیات با زمینه مشخص و بازگرداندن نتیجه.
     *
     * @param principal هویت جاری
     * @param action    عملیاتی که داخل محدوده اجرا می‌شود
     * @param <T>       نوع نتیجه
     * @return نتیجه {@code action}
     */
    public static <T> T callAs(TenantPrincipal principal, Supplier<T> action) {
        Objects.requireNonNull(principal, "principal نباید null باشد");
        Objects.requireNonNull(action, "action نباید null باشد");
        try {
            return ScopedValue.where(PRINCIPAL, principal).call(action::get);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Callable تنها می‌تواند Exception پرتاب کند؛ Supplier نمی‌تواند.
            // پس این شاخه فقط با خطای غیرمنتظره موتور ScopedValue پر می‌شود.
            throw new IllegalStateException("خطای غیرمنتظره در محدوده مستأجر", e);
        }
    }

    /**
     * اجرای یک عملیات بدون مقدار بازگشتی با زمینه مشخص.
     *
     * @param principal هویت جاری
     * @param action    عملیاتی که داخل محدوده اجرا می‌شود
     */
    public static void runAs(TenantPrincipal principal, Runnable action) {
        Objects.requireNonNull(principal, "principal نباید null باشد");
        Objects.requireNonNull(action, "action نباید null باشد");
        ScopedValue.where(PRINCIPAL, principal).run(action);
    }

    /**
     * اجرای عملیاتی که استثنای checked پرتاب می‌کند.
     *
     * <p>برای فیلتر Servlet لازم است: {@code chain.doFilter} هر دو
     * {@code ServletException} و {@code IOException} پرتاب می‌کند و
     * باید بدون تغییر به کانتینر منتشر شود.
     *
     * @param principal هویت جاری
     * @param action    عملیات
     * @throws Exception همان استثنای {@code action}، بدون پیچیدن
     */
    public static void runAsChecked(TenantPrincipal principal, CheckedRunnable action)
            throws Exception {
        Objects.requireNonNull(principal, "principal نباید null باشد");
        Objects.requireNonNull(action, "action نباید null باشد");
        try {
            ScopedValue.where(PRINCIPAL, principal).run(() -> {
                try {
                    action.run();
                } catch (Throwable t) {
                    throw new CheckedExceptionCarrier(t);
                }
            });
        } catch (CheckedExceptionCarrier carrier) {
            Throwable cause = carrier.getCause();
            if (cause instanceof Exception e) {
                throw e;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        }
    }

    /** عملیاتی که می‌تواند استثنای checked پرتاب کند. */
    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }

    /**
     * حامل داخلی برای عبور استثنای checked از میان {@link Runnable}.
     * هرگز از مرز این کلاس بیرون نمی‌رود.
     */
    private static final class CheckedExceptionCarrier extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CheckedExceptionCarrier(Throwable cause) {
            super(cause);
        }
    }

    // ─────────────────────────── خواندن زمینه ───────────────────────────

    /** هویت جاری، اگر در محدوده‌ای تعریف شده باشد. */
    public static Optional<TenantPrincipal> principal() {
        return Optional.ofNullable(peek());
    }

    /**
     * هویت جاری.
     *
     * @throws TenantNotResolvedException اگر درخواست احراز هویت نشده باشد
     */
    public static TenantPrincipal requirePrincipal() {
        TenantPrincipal principal = peek();
        if (principal == null) {
            throw new TenantNotResolvedException("خواندن هویت جاری");
        }
        return principal;
    }

    /**
     * شناسه مستأجر جاری.
     *
     * <p>در دامنه پلتفرم {@link Optional#empty()} برمی‌گردد — این با
     * «دسترسی به همه مستأجرها» یکی نیست.
     */
    public static Optional<UUID> currentTenantId() {
        TenantPrincipal principal = peek();
        return (principal == null) ? Optional.empty() : Optional.ofNullable(principal.tenantId());
    }

    /**
     * شناسه مستأجر جاری — برای مسیرهایی که بدون مستأجر معنا ندارند.
     *
     * <p><b>Fail-Closed:</b> اگر مستأجری نباشد، عملیات متوقف می‌شود
     * تا هرگز کوئری بدون فیلتر اجرا نشود.
     *
     * @throws TenantNotResolvedException اگر مستأجری در زمینه نباشد
     */
    public static UUID requireTenantId() {
        return currentTenantId().orElseThrow(
                () -> new TenantNotResolvedException("خواندن شناسه مستأجر جاری"));
    }

    /** شناسه کاربر جاری، اگر وارد شده باشد. */
    public static Optional<UUID> currentUserId() {
        TenantPrincipal principal = peek();
        return (principal == null) ? Optional.empty() : Optional.ofNullable(principal.userId());
    }

    /**
     * شناسه کاربر جاری.
     *
     * @throws TenantNotResolvedException اگر کاربری در زمینه نباشد
     */
    public static UUID requireUserId() {
        return currentUserId().orElseThrow(
                () -> new TenantNotResolvedException("خواندن شناسه کاربر جاری"));
    }

    /** آیا زمینه جاری روی دامنه پلتفرم است (بدون مستأجر)؟ */
    public static boolean isPlatformScope() {
        TenantPrincipal principal = peek();
        return principal != null && !principal.hasTenant();
    }

    /**
     * مقدار مؤثر برای Hibernate.
     *
     * <p>هرگز {@code null} برنمی‌گرداند — دلیلش در توضیح
     * {@link #PLATFORM_SENTINEL} آمده است.
     */
    public static UUID effectiveTenantIdForHibernate() {
        return currentTenantId().orElse(PLATFORM_SENTINEL);
    }

    private static TenantPrincipal peek() {
        // ⚠️ تنها نقطه وابسته به API نسخه ۲۵ در این کلاس. اگر بیلد اول
        // این خط را نپذیرفت، جایگزین معادل:
        //     try { return PRINCIPAL.get(); }
        //     catch (NoSuchElementException e) { return null; }
        return PRINCIPAL.isBound() ? PRINCIPAL.get() : null;
    }
}
