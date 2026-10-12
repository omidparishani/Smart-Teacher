package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaChannelRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateEitaaChannelRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.application.EitaaOutboundService;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * فاز ۹ / گام A2 — ویرایش و حذف مقصدهای ایتا.
 *
 * <p>سه چیز اینجا سنجیده می‌شود که شکستنشان بی‌صدا است و در نتیجه
 * خطرناک:
 *
 * <ol>
 *   <li><b>قرارداد سه‌حالتهٔ ویرایش</b> — {@code null} یعنی «تغییر نده» و
 *       رشتهٔ خالی یعنی «پاک کن». اگر این دو یکی می‌شدند، ویرایش «فقط
 *       عنوان» بی‌صدا {@code username} را پاک می‌کرد.</li>
 *   <li><b>حذف نرم</b> — اگر {@code softDelete()} کار نکند یا فهرست
 *       {@code deleted_at is null} را فیلتر نکند، مقصدِ حذف‌شده بی‌صدا
 *       باقی می‌ماند و کاربر فکر می‌کند حذف شده.</li>
 *   <li><b>جداسازی مستأجر</b> — ویرایش/حذف با شناسهٔ مستأجر دیگر باید
 *       «یافت نشد» بدهد، نه اینکه کار کند (خواستهٔ بخش ۵۶ — IDOR).</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EitaaOutboundCrudTest {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EitaaOutboundService outboundService;

    // ─── قرارداد سه‌حالتهٔ ویرایش گروه ───

    @Test
    @DisplayName("ویرایش فقط عنوان — chat_id دست‌نخورده می‌ماند و خودش را تکراری نمی‌بیند")
    void updateGroup_titleOnly_keepsChatId() {
        Actor teacher = newTenant();
        String chatId = "@grp-" + unique();

        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "عنوان قدیمی", "GROUP")));

        // 🔴 `eitaaChatId = null` یعنی «تغییر نده». اگر بررسی تکراری‌بودن
        // فیلتر «به‌جز خودم» نداشت، این فراخوانی باید رد می‌شد.
        var updated = TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateGroup(created.id(),
                        new UpdateEitaaGroupRequest(null, "عنوان جدید", null)));

        assertThat(updated.eitaaChatId()).isEqualTo(chatId);
        assertThat(updated.title()).isEqualTo("عنوان جدید");
        assertThat(updated.type()).isEqualTo("GROUP");
    }

    @Test
    @DisplayName("ویرایش با رشته خالی، عنوان را پاک می‌کند (نه اینکه تغییر ندهد)")
    void updateGroup_blankTitle_clearsIt() {
        Actor teacher = newTenant();

        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest("@grp-" + unique(), "عنوان", "GROUP")));

        var updated = TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateGroup(created.id(),
                        new UpdateEitaaGroupRequest(null, "   ", null)));

        assertThat(updated.title()).isNull();
    }

    @Test
    @DisplayName("type خالی عنوان را تغییر نمی‌دهد — چون ستون NOT NULL است")
    void updateGroup_blankType_isNoOp() {
        Actor teacher = newTenant();

        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest("@grp-" + unique(), "عنوان", "GROUP")));

        // اگر اینجا `type` را `null` می‌کردیم، commit با استثنای
        // محدودیت NOT NULL می‌شکست.
        var updated = TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateGroup(created.id(),
                        new UpdateEitaaGroupRequest(null, null, "")));

        assertThat(updated.type()).isEqualTo("GROUP");
    }

    @Test
    @DisplayName("فرستادن chat_id خودِ همان رکورد → موفق (خودش را تکراری نمی‌بیند)")
    void updateGroup_ownChatIdIsNotADuplicate() {
        Actor teacher = newTenant();
        String chatId = "@self-" + unique();

        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "عنوان", "GROUP")));

        // 🔴 این تنها ورودی‌ای است که `...AndIdNot` را از نسخهٔ بدون آن
        // جدا می‌کند: مقدار <b>غیرnull</b> است (پس شاخهٔ بررسی وارد
        // می‌شود) ولی به رکورد دیگری اشاره نمی‌کند. اگر کسی `AndIdNot` را
        // بردارد، این آزمون — و فقط این آزمون — می‌شکند.
        var updated = TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateGroup(created.id(),
                        new UpdateEitaaGroupRequest(chatId, "عنوان ۲", null)));

        assertThat(updated.eitaaChatId()).isEqualTo(chatId);
        assertThat(updated.title()).isEqualTo("عنوان ۲");
    }

    @Test
    @DisplayName("تغییر chat_id به شناسه‌ای که همان مستأجر دارد → DuplicateResource")
    void updateGroup_chatIdTakenInSameTenant_rejected() {
        Actor teacher = newTenant();
        String taken = "@taken-" + unique();

        TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(taken, "اولی", "GROUP")));
        var second = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest("@grp-" + unique(), "دومی", "GROUP")));

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateGroup(second.id(),
                        new UpdateEitaaGroupRequest(taken, null, null))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("تغییر chat_id به شناسه‌ای که مستأجر دیگری دارد هم رد می‌شود")
    void updateGroup_chatIdTakenByAnotherTenant_rejected() {
        Actor tenantA = newTenant();
        Actor tenantB = newTenant();
        String shared = "@shared-" + unique();

        TenantContext.callAs(tenantA.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(shared, "مال A", "GROUP")));
        var groupOfB = TenantContext.callAs(tenantB.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest("@grp-" + unique(), "مال B", "GROUP")));

        // 🔴 این آزمون یک ناسازگاری واقعی را قفل می‌کند: یکتایی در پایگاه
        // داده <b>سراسری</b> است (`uk_eitaa_groups_chat_id`)، ولی
        // `@TenantId` دامنهٔ کوئری JPQL را تنگ می‌کرد. با بررسی
        // tenant-scoped، این فراخوانی از بررسی برنامه رد می‌شد و بعد در
        // `commit` با نقض یکتایی می‌افتاد → پیام گمراه‌کنندهٔ ۴۰۹
        // «تناقض وضعیت» به‌جای «قبلاً ثبت شده». بررسی native حالا هم‌دامنهٔ
        // پایگاه داده است، پس همان‌جا و با پیام درست رد می‌شود.
        assertThatThrownBy(() -> TenantContext.callAs(tenantB.principal(), () ->
                outboundService.updateGroup(groupOfB.id(),
                        new UpdateEitaaGroupRequest(shared, null, null))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("ساخت گروه با chat_id تکراری در مستأجر دیگر هم رد می‌شود (POST ≡ PUT)")
    void createGroup_duplicateAcrossTenants_rejected() {
        Actor tenantA = newTenant();
        Actor tenantB = newTenant();
        String shared = "@cross-" + unique();

        TenantContext.callAs(tenantA.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(shared, "مال A", "GROUP")));

        assertThatThrownBy(() -> TenantContext.callAs(tenantB.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest(shared, "مال B", "GROUP"))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("ویرایش گروه با chat_id نامعتبر → VALIDATION_FAILED با پیام راهنما")
    void updateGroup_invalidChatId_rejected() {
        Actor teacher = newTenant();
        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest("@grp-" + unique(), "عنوان", "GROUP")));

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateGroup(created.id(),
                        new UpdateEitaaGroupRequest("https://eitaa.com/foo", null, null))))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(ex.getMessage()).contains("بدون @");
                });
    }

    // ─── حذف نرم ───

    @Test
    @DisplayName("حذف گروه — از فهرست ناپدید می‌شود ولی رکورد فیزیکی می‌ماند")
    void deleteGroup_softDeletes() {
        Actor teacher = newTenant();
        String chatId = "@del-" + unique();

        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "حذفی", "GROUP")));

        TenantContext.callAs(teacher.principal(), () -> {
            outboundService.deleteGroup(created.id());
            return null;
        });

        assertThat(TenantContext.callAs(teacher.principal(), () ->
                outboundService.listGroups()))
                .noneMatch(g -> g.id().equals(created.id()));

        // ⚠️ رکورد باید باقی بماند — حذف نرم است، نه فیزیکی.
        Integer remaining = jdbc.queryForObject(
                "select count(*) from eitaa_groups where id = ? and deleted_at is not null",
                Integer.class, created.id());
        assertThat(remaining).isEqualTo(1);
    }

    @Test
    @DisplayName("حذف گروه حذف‌شده دوباره → «یافت نشد» (نه حذف دوبارهٔ بی‌صدا)")
    void deleteGroup_twice_isNotFound() {
        Actor teacher = newTenant();
        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest("@del-" + unique(), "حذفی", "GROUP")));

        TenantContext.callAs(teacher.principal(), () -> {
            outboundService.deleteGroup(created.id());
            return null;
        });

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () -> {
            outboundService.deleteGroup(created.id());
            return null;
        })).isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── قرارداد سه‌حالتهٔ ویرایش کانال ───

    @Test
    @DisplayName("ویرایش کانال — title و username جداگانه پاک/دست‌نخورده می‌شوند")
    void updateChannel_titleAndUsernameAreIndependent() {
        Actor teacher = newTenant();

        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createChannel(new CreateEitaaChannelRequest(
                        "@ch-" + unique(), "عنوان", "my_user")));

        // فقط username پاک می‌شود؛ title باید دست‌نخورده بماند.
        var afterClear = TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateChannel(created.id(),
                        new UpdateEitaaChannelRequest(null, null, "")));
        assertThat(afterClear.username()).isNull();
        assertThat(afterClear.title()).isEqualTo("عنوان");

        // فقط title عوض می‌شود؛ username (که حالا null است) نباید برگردد.
        var afterTitle = TenantContext.callAs(teacher.principal(), () ->
                outboundService.updateChannel(created.id(),
                        new UpdateEitaaChannelRequest(null, "عنوان ۲", null)));
        assertThat(afterTitle.title()).isEqualTo("عنوان ۲");
        assertThat(afterTitle.username()).isNull();
        assertThat(afterTitle.eitaaChatId()).isEqualTo(created.eitaaChatId());
    }

    @Test
    @DisplayName("حذف کانال — نرم و خارج از فهرست")
    void deleteChannel_softDeletes() {
        Actor teacher = newTenant();

        var created = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createChannel(new CreateEitaaChannelRequest(
                        "@ch-" + unique(), "کانال حذفی", null)));

        TenantContext.callAs(teacher.principal(), () -> {
            outboundService.deleteChannel(created.id());
            return null;
        });

        assertThat(TenantContext.callAs(teacher.principal(), () ->
                outboundService.listChannels()))
                .noneMatch(c -> c.id().equals(created.id()));
    }

    // ─── جداسازی مستأجر (بخش ۵۶ — IDOR) ───

    @Test
    @DisplayName("مستأجر B نمی‌تواند گروه مستأجر A را ویرایش یا حذف کند")
    void otherTenant_cannotUpdateOrDelete() {
        Actor tenantA = newTenant();
        Actor tenantB = newTenant();

        var groupOfA = TenantContext.callAs(tenantA.principal(), () ->
                outboundService.createGroup(
                        new CreateEitaaGroupRequest("@iso-" + unique(), "مال A", "GROUP")));

        // 🔴 انتظار «یافت نشد» است، نه «ممنوع» — افشای وجودِ رکورد
        // مستأجر دیگر خودش یک نشت اطلاعاتی است.
        assertThatThrownBy(() -> TenantContext.callAs(tenantB.principal(), () ->
                outboundService.updateGroup(groupOfA.id(),
                        new UpdateEitaaGroupRequest(null, "دزدیده", null))))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> TenantContext.callAs(tenantB.principal(), () -> {
            outboundService.deleteGroup(groupOfA.id());
            return null;
        })).isInstanceOf(ResourceNotFoundException.class);

        // و رکورد A دست‌نخورده مانده است.
        var untouched = TenantContext.callAs(tenantA.principal(), () ->
                outboundService.listGroups());
        assertThat(untouched).anySatisfy(g -> {
            assertThat(g.id()).isEqualTo(groupOfA.id());
            assertThat(g.title()).isEqualTo("مال A");
        });
    }

    // ─── بستر آزمون ───

    private Actor newTenant() {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "کاربر " + unique());
        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """,
                tenantId, "مدرسه " + unique(), "school-" + unique(), userId);
        return new Actor(userId, tenantId, TenantPrincipal.ofTenantUser(
                userId, tenantId, null, Set.of(MemberRole.TEACHER.name()),
                MemberRole.TEACHER, "fa"));
    }

    private static String unique() {
        return "c" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
