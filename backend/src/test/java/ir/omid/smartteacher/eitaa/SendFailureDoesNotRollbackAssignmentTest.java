package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.EitaaApiException;
import ir.omid.smartteacher.eitaa.internal.EitaaHttpClient;
import ir.omid.smartteacher.eitaa.internal.EitaaYarBotAdapter;
import ir.omid.smartteacher.eitaa.internal.application.EitaaAccountProvisioner;
import ir.omid.smartteacher.user.UserRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * قطعی ایتا نباید کارِ ذخیره‌شده معلم را از بین ببرد.
 *
 * <p><b>🔴 حالتی که این کلاس برای گرفتنش نوشته شده:</b> کاری که معلم
 * انجام داده در یک تراکنش دیتابیس ذخیره می‌شود و در همان تراکنش، اعلان
 * به ایتا می‌رود. اگر ارسال ایتا <i>استثنا</i> پرتاب کند، تراکنش
 * برمی‌گردد و کار معلم از بین می‌رود — در حالی که کار او هیچ ایرادی
 * نداشت. {@code docs/eitaa.md} بخش ۹ صریح می‌گوید: «تکلیف در سیستم ما
 * ذخیره شده؛ ایتا فقط یک کانال توزیع است.» پس شکست ارسال باید یک
 * <b>مقدار</b> باشد که فراخوان درباره‌اش تصمیم بگیرد، نه استثنایی که
 * تراکنش را می‌کشد.
 *
 * <p><b>⚠️ «تکلیف» فاز ۷ است و هنوز وجود ندارد.</b> پس این کلاس
 * <i>مکانیزم</i> را می‌سنجد، نه یک موجودیت خاص را: یک نوشتن واقعی
 * چندجدولی روی PostgreSQL واقعی
 * ({@link EitaaAccountProvisioner#findOrCreate}) در همان تراکنشی که
 * ارسال ایتا شکست می‌خورد. شکل مسئله یکی است — چند {@code INSERT} با
 * کلید خارجی، به‌اضافه یک ارسال شکست‌خورده — و اگر مکانیزم درست باشد،
 * روزی که تکلیف اضافه شود همان مکانیزم از آن محافظت می‌کند. آزمون روی
 * یک شبیه‌ساز نوشته نشده چون شبیه‌ساز هیچ تراکنشی ندارد و هیچ چیزی را
 * ثابت نمی‌کرد.
 *
 * <p><b>⚠️ چرا PostgreSQL واقعی؟</b> چون ادعای «تراکنش برنمی‌گردد» فقط
 * با یک دیتابیس واقعی معنا دارد. هیچ پروفایل in-memory در این پروژه
 * نیست و Migrationها هم باید واقعاً اجرا شوند تا کلیدهای خارجی و
 * محدودیت‌ها همان‌طور که در Production هستند سنجیده شوند.
 *
 * <p><b>⚠️ {@code @Transactional} روی کلاس نیست — عمدی.</b> اگر بود،
 * Spring یک تراکنش بیرونی باز می‌کرد و <i>همه</i> آزمون‌ها در پایان
 * rollback می‌شدند؛ آن‌وقت «ذخیره شد» و «ذخیره نشد» هر دو یک نتیجه
 * می‌دادند و آزمون بی‌معنا می‌گشت. مرز تراکنش اینجا دستی و صریح است.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SendFailureDoesNotRollbackAssignmentTest {

    /** همان تصویر Production؛ با {@code -Dtest.postgres.image} قابل تغییر. */
    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final String GROUP = "@teaching_group";

    /**
     * شناسه‌های ایتا یکتا برای هر آزمون.
     *
     * <p>⚠️ لازم است: {@code eitaa_identities.eitaa_user_id} یکتا است و
     * رکوردها بین آزمون‌ها نمی‌روند (چون rollback خودکار نداریم). با
     * شناسه مشترک، آزمون دوم کاربر موجود را پیدا می‌کرد و آن‌وقت
     * «ساخته شد؟» دیگر سنجیده نمی‌شد.
     *
     * <p>همه از یک پایه ۵۲ بیتی شروع می‌شوند — همان محدوده‌ای که سند
     * رسمی برای {@code user.id} گفته، تا آزمون خودش هم بیرون از
     * محدوده واقعی کار نکند.
     */
    private static final long ID_FAILED_SEND = 4_503_599_627_370_001L;
    private static final long ID_OK_SEND = 4_503_599_627_370_002L;
    private static final long ID_ESCAPING_EXCEPTION = 4_503_599_627_370_003L;
    private static final long ID_HOSTILE_RESPONSES = 4_503_599_627_370_004L;
    private static final long ID_CAPABILITY_MISSING = 4_503_599_627_370_005L;

    @Autowired
    private EitaaAccountProvisioner provisioner;

    @Autowired
    private EitaaIdentityRepository identities;

    @Autowired
    private UserRepository users;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private EitaaStubServer server;
    private EitaaYarBotAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = EitaaStubServer.start();
        adapter = EitaaRealStack.botAdapter(server.properties());
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    // ─────────────────────────── ابزار ───────────────────────────

    /** یک تراکنش تازه — مرز آن چیزی است که باید بماند یا برگردد. */
    private TransactionTemplate newTransaction() {
        return new TransactionTemplate(transactionManager);
    }

    private static EitaaUserData userData(long eitaaUserId) {
        return new EitaaUserData(
                eitaaUserId, "زهرا", "معلم نمونه", null, "fa", Boolean.FALSE, Boolean.TRUE);
    }

    /** نشانی کامل مسیر الف — حاوی توکن است و فقط داخل همین آزمون می‌ماند. */
    private String botUrl() {
        return server.baseUrl() + "/" + EitaaTestData.BOT_TOKEN + "/sendMessage";
    }

    /**
     * صدا زدن ارسال به‌شکلی که یک استثنای ناخواسته، پیام روشنی بدهد.
     *
     * <p>⚠️ بدون این، شکست آزمون فقط یک stack trace از داخل کتابخانه
     * بود و نمی‌گفت کدام شکل پاسخ باعثش شده.
     */
    private static EitaaSendResult sendExpectingFailure(
            Supplier<EitaaSendResult> send, String label) {
        try {
            return send.get();
        } catch (RuntimeException unexpected) {
            throw new AssertionError(
                    "«" + label + "»: شکست ارسال به استثنا تبدیل شد ("
                            + unexpected.getClass().getSimpleName()
                            + ") — قرارداد EitaaAdapter این را اجازه نمی‌دهد",
                    unexpected);
        }
    }

    // ─────────────────────────── آزمون‌های اصلی ───────────────────────────

    @Test
    @DisplayName("شکست ارسال ایتا، کار ذخیره‌شده معلم را برنمی‌گرداند")
    void failedSendDoesNotRollBackTheSavedWork() {
        // 🔴 آزمون اصلی این کلاس. ایتا پاسخ ناموفق می‌دهد و ارسال شکست
        // می‌خورد — ولی رکوردهای همان تراکنش باید سر جایشان بمانند.
        server.respondNotOk("403", "bot was blocked by the user");

        EitaaSendResult[] sendResult = new EitaaSendResult[1];

        newTransaction().executeWithoutResult(status -> {
            provisioner.findOrCreate(userData(ID_FAILED_SEND));
            sendResult[0] = adapter.sendToChat(null, GROUP, "تکلیف تازه برای فردا", null);
        });

        assertThat(sendResult[0].success())
                .as("ارسال واقعاً شکست خورده باشد، وگرنه آزمون چیزی را نمی‌سنجد")
                .isFalse();
        assertThat(sendResult[0].errorCode()).isEqualTo("403");

        assertThat(identities.findByEitaaUserId(ID_FAILED_SEND))
                .as("کار معلم باید ذخیره شده باشد حتی وقتی ارسال به ایتا شکست خورد")
                .isPresent();

        EitaaIdentity identity = identities.findByEitaaUserId(ID_FAILED_SEND).orElseThrow();

        assertThat(users.findById(identity.getUserId()))
                .as("نوشتن‌های پیشین همان تراکنش هم باید مانده باشند، نه فقط آخری")
                .isPresent();
    }

    @Test
    @DisplayName("اگر همان استثنا بیرون می‌زد، همان کار برمی‌گشت")
    void theEscapingExceptionWouldHaveRolledBackTheSameWork() {
        // 🔴 این آزمون ثابت می‌کند آزمون بالا <b>بیمعنا نیست</b> — یعنی
        // واقعاً چیزی را محافظت می‌کند.
        //
        // آزمون بالا به‌تنهایی می‌توانست با یک تراکنش که هیچ‌وقت rollback
        // نمی‌شود هم سبز شود. اینجا نشان می‌دهیم که همان کار، در همان
        // نوع تراکنش، <i>اگر</i> آن استثنا بیرون بزند از بین می‌رود.
        // این دقیقاً چیزی است که پیش از اصلاح قرارداد رخ می‌داد.
        server.respondNotOk("403", "bot was blocked by the user");

        // ۱) قرارداد فعلی: هیچ استثنایی از مسیر ارسال بیرون نمی‌زند.
        EitaaSendResult result = sendExpectingFailure(
                () -> adapter.sendToChat(null, GROUP, "تکلیف", null), "پاسخ ok:false");

        assertThat(result.success()).isFalse();

        // ۲) همان استثنایی که لایه HTTP تولید می‌کند — اینجا عمداً
        //    گرفته <i>نمی‌شود</i> تا رفتار پیش از اصلاح بازسازی شود.
        //    مستقیم به لایه HTTP می‌زنیم چون مسیر ارسال دیگر آن را
        //    بیرون نمی‌دهد.
        EitaaHttpClient httpClient = EitaaRealStack.httpClient(server.properties());

        UUID[] createdUserId = new UUID[1];

        assertThatThrownBy(() -> newTransaction().executeWithoutResult(status -> {
            createdUserId[0] = provisioner.findOrCreate(userData(ID_ESCAPING_EXCEPTION))
                    .user().getId();

            httpClient.postJson(botUrl(), "sendMessage", "probe",
                    Map.of("chat_id", GROUP, "text", "تکلیف"));
        }))
                .as("لایه HTTP شکست را با استثنا علامت می‌دهد")
                .isInstanceOf(EitaaApiException.class);

        assertThat(identities.findByEitaaUserId(ID_ESCAPING_EXCEPTION))
                .as("استثنای بیرون‌زده تراکنش را برگرداند")
                .isEmpty();
        assertThat(users.findById(createdUserId[0]))
                .as("کاربر هم برگشته باشد — rollback کامل است، نه نیمه‌کاره")
                .isEmpty();
    }

    @Test
    @DisplayName("ارسال موفق هم همان کار را ذخیره می‌کند")
    void successfulSendAlsoCommits() {
        // ⚠️ لنگر آزمون: اگر ذخیره‌شدن فقط در مسیر شکست درست باشد،
        // معلوم نیست تراکنش سالم است یا اتفاق دیگری افتاده.
        server.respondOkWith("{\"message_id\":\"85917\"}");

        EitaaSendResult[] sendResult = new EitaaSendResult[1];

        newTransaction().executeWithoutResult(status -> {
            provisioner.findOrCreate(userData(ID_OK_SEND));
            sendResult[0] = adapter.sendToChat(null, GROUP, "تکلیف", null);
        });

        assertThat(sendResult[0].success()).isTrue();
        assertThat(identities.findByEitaaUserId(ID_OK_SEND)).isPresent();
    }

    // ─────────────────────────── قرارداد: مقدار، نه استثنا ───────────────────────────

    @Test
    @DisplayName("هیچ شکل پاسخی، شکست ارسال را به استثنا تبدیل نمی‌کند")
    void noResponseShapeTurnsSendFailureIntoAnException() {
        // 🔴 هر شکلی از پاسخ که ایتا ممکن است بدهد — از جمله پاسخ‌های
        // بدشکل و کدهای وضعیت خطا. هیچ‌کدام نباید استثنا بدهند، چون
        // استثنا یعنی rollback تراکنش و از بین رفتن کار معلم.
        List<HostileResponse> hostile = List.of(
                new HostileResponse(200, "{\"ok\":false,\"error_code\":\"403\","
                        + "\"description\":\"blocked\"}", "ok:false در HTTP ۲۰۰"),
                new HostileResponse(200, "{\"ok\":false}", "ok:false بدون error_code"),
                new HostileResponse(200, "{\"result\":{\"message_id\":\"1\"}}",
                        "پاسخ بدون فیلد ok"),
                new HostileResponse(200, "", "بدنه خالی"),
                new HostileResponse(200, "<html><body>502 Bad Gateway</body></html>",
                        "بدنه غیر-JSON"),
                new HostileResponse(200, "{\"ok\":tr", "JSON ناقص"),
                new HostileResponse(500, "{\"ok\":false}", "کد وضعیت ۵۰۰"),
                new HostileResponse(401, "", "توکن نامعتبر"),
                new HostileResponse(429, "{\"ok\":false}", "محدودیت نرخ"));

        EitaaFilePayload file =
                new EitaaFilePayload("t.pdf", "application/pdf", new byte[]{1, 2, 3});

        for (HostileResponse response : hostile) {
            server.respondWith(response.status(), response.body());

            EitaaSendResult text = sendExpectingFailure(
                    () -> adapter.sendToChat(null, GROUP, "تکلیف", null),
                    response.label() + " روی ارسال متن");

            EitaaSendResult sentFile = sendExpectingFailure(
                    () -> adapter.sendFileToChat(null, GROUP, file, "توضیح", null),
                    response.label() + " روی ارسال فایل");

            EitaaSendResult privateMessage = sendExpectingFailure(
                    () -> adapter.sendToUser(null, 555L, "متن"),
                    response.label() + " روی مسیر ب");

            assertThat(text.success()).as("%s: متن", response.label()).isFalse();
            assertThat(sentFile.success()).as("%s: فایل", response.label()).isFalse();
            assertThat(privateMessage.success()).as("%s: خصوصی", response.label()).isFalse();

            // ⚠️ کد خطا نباید خالی بماند: مصرف‌کننده روی همان تصمیم
            // می‌گیرد (هشدار دادن، تلاش دوباره، یا فقط ثبت کردن).
            assertThat(text.errorCode()).as("%s: کد خطای متن", response.label()).isNotBlank();
            assertThat(sentFile.errorCode())
                    .as("%s: کد خطای فایل", response.label())
                    .isNotBlank();
            assertThat(privateMessage.errorCode())
                    .as("%s: کد خطای مسیر ب", response.label())
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("خطای «این کار ممکن نیست» همچنان استثنا است و تراکنش را برمی‌گرداند")
    void capabilityFailureIsStillAnExceptionAndRollsBack() {
        // 🔴 این آزمون مرز اصلاح را نشان می‌دهد. اگر کسی همه‌چیز را در
        // try/catch می‌پیچید، اینجا معلوم می‌شد: «قابلیت وجود ندارد» با
        // «الآن نشد» یکی نیست. اولی یعنی برنامه باید عوض شود، پس باید
        // بلند باشد — نه یک مقدار ناموفق که کسی نادیده بگیرد.
        EitaaProperties botTokenOnly = EitaaTestData.withBaseUrl(
                EitaaTestData.botProperties(), server.baseUrl(), server.appBaseUrl());
        EitaaYarBotAdapter botOnlyAdapter = EitaaRealStack.botAdapter(botTokenOnly);

        UUID[] createdUserId = new UUID[1];

        assertThatThrownBy(() -> newTransaction().executeWithoutResult(status -> {
            createdUserId[0] = provisioner.findOrCreate(userData(ID_CAPABILITY_MISSING))
                    .user().getId();

            // توکن برنامک تنظیم نشده، پس ارسال خصوصی ممکن نیست.
            botOnlyAdapter.sendToUser(null, 555L, "اعلان خصوصی");
        }))
                .as("نبود قابلیت، «الآن نشد» نیست؛ «ممکن نیست» است")
                .isInstanceOf(EitaaCapabilityNotSupportedException.class);

        assertThat(identities.findByEitaaUserId(ID_CAPABILITY_MISSING)).isEmpty();
        assertThat(users.findById(createdUserId[0]))
                .as("وقتی خطا عمداً بلند می‌شود، تراکنش هم واقعاً برمی‌گردد")
                .isEmpty();
    }

    // ─────────────────────────── آزمون‌های کمکی ───────────────────────────

    @Test
    @DisplayName("شناسه‌های آزمون‌های منفی، هیچ‌وقت در دیتابیس نمی‌مانند")
    void rolledBackIdsAreNeverInTheDatabase() {
        // ⚠️ آزمون نگهبان: «غایب بود» در آزمون‌های منفی باید یعنی
        // «واقعاً چیزی ساخته نشد» — نه «شناسه اشتباهی پرسیده شد» یا
        // «کوئری اشتباه اجرا شد». بدون این، یک اشتباه تایپی در
        // شناسه‌ها همه آزمون‌های منفی را بی‌صدا سبز می‌کرد.
        //
        // ⚠️ شناسه‌های ID_FAILED_SEND و ID_OK_SEND عمداً در این فهرست
        // نیستند: آن دو <i>باید</i> بمانند. اگر اینجا بررسی می‌شدند،
        // نتیجه آزمون به ترتیب اجرای تست‌ها وابسته می‌شد و شکننده بود.
        //
        // ⚠️ این آزمون به ترتیب اجرا وابسته نیست: چه پیش از آزمون‌های
        // دیگر اجرا شود (هرگز ساخته نشده‌اند) و چه پس از آن‌ها (ساخته
        // شدند و برگشتند)، غایب‌اند.
        for (long id : new long[]{
                ID_ESCAPING_EXCEPTION, ID_HOSTILE_RESPONSES, ID_CAPABILITY_MISSING}) {

            assertThat(identities.existsByEitaaUserId(id))
                    .as("شناسه %d نباید هیچ‌وقت در دیتابیس بماند", id)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("بازگشت‌کردن تراکنش، رکوردهای نوشته‌شده را واقعاً برمی‌گرداند")
    void rollbackActuallyRemovesRows() {
        // ⚠️ آزمون نگهبان دوم، در جهت مخالف: ثابت می‌کند تراکنش این
        // دیتابیس <i>می‌تواند</i> برگردد. اگر کسی روزی ddl یا موتور
        // تراکنش را جوری عوض کند که rollback بی‌اثر شود، آزمون‌های
        // «استثنا تراکنش را برگرداند» بی‌معنا می‌شدند و اینجا معلوم
        // می‌شود.
        UUID[] createdUserId = new UUID[1];

        assertThatThrownBy(() -> newTransaction().executeWithoutResult(status -> {
            createdUserId[0] = provisioner.findOrCreate(userData(ID_HOSTILE_RESPONSES))
                    .user().getId();
            throw new IllegalStateException("بازگشت عمدی برای سنجش");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(identities.findByEitaaUserId(ID_HOSTILE_RESPONSES)).isEmpty();
        assertThat(users.findById(createdUserId[0])).isEmpty();
    }

    /** یک پاسخ بدخیم ایتا — کد وضعیت و بدنه‌اش. */
    private record HostileResponse(int status, String body, String label) {
    }
}
