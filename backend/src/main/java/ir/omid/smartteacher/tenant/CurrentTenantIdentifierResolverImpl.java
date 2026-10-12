package ir.omid.smartteacher.tenant;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * به Hibernate می‌گوید «مستأجر جاری کدام است».
 *
 * <p>Hibernate این متد را برای هر Session صدا می‌زند و نتیجه را در
 * شرط {@code tenant_id = ...} همه کوئری‌های موجودیت‌های
 * {@link TenantAwareEntity} به کار می‌برد.
 *
 * <p><b>⚠️ چرا هرگز استثنا پرتاب نمی‌کند؟</b> چون Hibernate این متد را
 * در بازکردن Session صدا می‌زند — حتی برای مسیرهای عمومی مثل ورود که
 * هنوز هیچ مستأجری وجود ندارد. اگر آنجا استثنا پرتاب شود، ورود کاربر
 * غیرممکن می‌شود و اپلیکیشن بالا نمی‌آید. پس در نبود مستأجر،
 * {@link TenantContext#PLATFORM_SENTINEL} برمی‌گردد: مقداری معتبر که به
 * هیچ رکوردی تعلق ندارد، پس کوئری‌ها خالی برمی‌گردند — همان نتیجه
 * Fail-Closed، بدون شکستن ورود.
 *
 * <p>اعمال سخت‌گیرانه‌ی «بدون مستأجر نباید نوشت» در لایه بالاتر انجام
 * می‌شود، جایی که می‌دانیم عملیات واقعاً به مستأجر نیاز دارد:
 * {@code TenantContext.requireTenantId()} و {@code TenantGuard}.
 */
@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.effectiveTenantIdForHibernate();
    }

    /**
     * بررسی تطابق مستأجر Session موجود با مستأجر جاری.
     *
     * <p>{@code true} یعنی اگر Session‌ای با مستأجر الف باز شده باشد و
     * کدی بخواهد با مستأجر ب از آن استفاده کند، Hibernate استثنا
     * می‌دهد. این یک تور اطمینان در برابر نشتی بین‌مستأجری در
     * Thread‌های بازاستفاده‌شده است و باید روشن بماند.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
