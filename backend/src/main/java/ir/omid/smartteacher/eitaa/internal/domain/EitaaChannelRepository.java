package ir.omid.smartteacher.eitaa.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EitaaChannelRepository extends JpaRepository<EitaaChannel, UUID> {

    List<EitaaChannel> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<EitaaChannel> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * آیا این {@code chat_id} در <b>هیچ</b> مستأجری ثبت شده است؟
     *
     * <p>🔴 {@code nativeQuery} عمدی است — دلیل کاملش در
     * {@link EitaaGroupRepository#existsByEitaaChatIdAnyTenant(String)} آمده:
     * یکتایی در پایگاه داده سراسری است
     * ({@code V25: uk_eitaa_channels_chat_id}), ولی فیلتر خودکار
     * {@code @TenantId} دامنهٔ کوئری JPQL را تنگ‌تر می‌کرد و بررسی برنامه
     * با محدودیت پایگاه داده هم‌دامنه نبود.
     */
    @Query(value = "select exists(select 1 from eitaa_channels c where c.eitaa_chat_id = :chatId)",
            nativeQuery = true)
    boolean existsByEitaaChatIdAnyTenant(@Param("chatId") String chatId);

    /** همان بررسی، به‌جز خودِ این رکورد — برای ویرایش. */
    @Query(value = "select exists(select 1 from eitaa_channels c "
            + "where c.eitaa_chat_id = :chatId and c.id <> :id)",
            nativeQuery = true)
    boolean existsByEitaaChatIdAnyTenantAndIdNot(@Param("chatId") String chatId,
                                                 @Param("id") UUID id);
}
