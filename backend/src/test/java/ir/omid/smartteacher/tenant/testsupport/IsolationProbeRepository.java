package ir.omid.smartteacher.tenant.testsupport;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * مخزن کاوشگر جداسازی. ⚠️ فقط تست.
 *
 * <p>متدهای مشتق‌شده ({@code countByLabel}) خودکار با فیلتر مستأجر
 * Hibernate اجرا می‌شوند. متد {@link #findRawTenantIdsByLabel} عمداً
 * این فیلتر را دور می‌زند — توضیحش پایین‌تر.
 */
public interface IsolationProbeRepository extends JpaRepository<IsolationProbe, UUID> {

    /** شمارش با فیلتر خودکار مستأجر. */
    long countByLabel(String label);

    /**
     * خواندن <b>خام</b> ستون {@code tenant_id} بدون فیلتر مستأجر.
     *
     * <p><b>⚠️ چرا این متد وجود دارد و چرا مهم است؟</b> دو دلیل:
     *
     * <ol>
     *   <li><b>اثبات مُهر خوردن:</b> باید ببینیم هنگام ذخیره، ستون
     *       {@code tenant_id} واقعاً مقدار مستأجر جاری را گرفته است.
     *       کوئری معمولی این را نشان نمی‌دهد چون خودش فیلتر شده و
     *       رکوردهای مستأجر دیگر را پنهان می‌کند.</li>
     *   <li><b>مستندسازی یک خطر واقعی:</b> Hibernate کوئری‌های
     *       <i>بومی</i> (native SQL) را بازنویسی نمی‌کند، پس فیلتر
     *       {@code @TenantId} روی آن‌ها اعمال <b>نمی‌شود</b>. هر کد
     *       آینده‌ای که SQL بومی بنویسد باید شرط {@code tenant_id} را
     *       دستی اضافه کند وگرنه داده مستأجرهای دیگر را می‌بیند. این
     *       تست همان قابلیت دور زدن را عمداً به کار می‌گیرد تا رفتار
     *       واقعی سیستم مستند و قابل مشاهده بماند.</li>
     * </ol>
     *
     * @param label برچسب رکورد
     * @return شناسه مستأجرهای ثبت‌شده روی رکوردهای با این برچسب
     */
    @Query(value = "SELECT tenant_id FROM isolation_probe WHERE label = :label",
            nativeQuery = true)
    List<UUID> findRawTenantIdsByLabel(@Param("label") String label);
}
