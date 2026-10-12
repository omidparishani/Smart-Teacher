package ir.omid.smartteacher.eitaa.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EitaaGroupRepository extends JpaRepository<EitaaGroup, UUID> {

    Optional<EitaaGroup> findByIdAndDeletedAtIsNull(UUID id);

    List<EitaaGroup> findByDeletedAtIsNullOrderByCreatedAtDesc();

    /**
     * آیا این {@code chat_id} در <b>هیچ</b> مستأجری ثبت شده است؟
     *
     * <p>🔴 عمداً {@code nativeQuery} است، نه یک متد مشتق‌شده. محدودیت
     * یکتایی در پایگاه داده <b>سراسری</b> است
     * ({@code V17: uk_eitaa_groups_chat_id UNIQUE (eitaa_chat_id)} — بدون
     * {@code tenant_id} و بدون شرط {@code deleted_at})، ولی Hibernate روی
     * هر کوئری JPQL خودکار شرط مستأجر را می‌گذارد (به‌خاطر
     * {@code @TenantId} در {@code TenantAwareEntity}). نتیجه یک ناسازگاری
     * بی‌صدا بود: متد مشتق‌شده برای رکوردِ <i>مستأجر دیگر</i> {@code false}
     * می‌داد، ولی {@code commit} با نقض یکتایی رد می‌شد و کاربر — به‌جای
     * «قبلاً ثبت شده» — پیام گمراه‌کنندهٔ «این عملیات با وضعیت فعلی سیستم
     * در تناقض است» می‌گرفت. کوئری native از فیلتر مستأجر Hibernate عبور
     * می‌کند و دقیقاً همان چیزی را می‌سنجد که پایگاه داده می‌سنجد.
     */
    @Query(value = "select exists(select 1 from eitaa_groups g where g.eitaa_chat_id = :chatId)",
            nativeQuery = true)
    boolean existsByEitaaChatIdAnyTenant(@Param("chatId") String chatId);

    /**
     * همان بررسی، به‌جز خودِ این رکورد — برای ویرایش.
     *
     * <p>بدون {@code and g.id <> :id}، ویرایشی که {@code chat_id} را
     * دست‌نخورده می‌گذارد (حالت رایج: فقط تغییر عنوان) خودش را تکراری
     * می‌دید و رد می‌شد.
     */
    @Query(value = "select exists(select 1 from eitaa_groups g "
            + "where g.eitaa_chat_id = :chatId and g.id <> :id)",
            nativeQuery = true)
    boolean existsByEitaaChatIdAnyTenantAndIdNot(@Param("chatId") String chatId,
                                                @Param("id") UUID id);
}
