package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.eitaa.EitaaAdapter;
import ir.omid.smartteacher.eitaa.EitaaAppToken;
import ir.omid.smartteacher.eitaa.EitaaBotInfo;
import ir.omid.smartteacher.eitaa.EitaaBotToken;
import ir.omid.smartteacher.eitaa.EitaaCapability;
import ir.omid.smartteacher.eitaa.EitaaFilePayload;
import ir.omid.smartteacher.eitaa.EitaaHealth;
import ir.omid.smartteacher.eitaa.EitaaNotification;
import ir.omid.smartteacher.eitaa.EitaaSendOptions;
import ir.omid.smartteacher.eitaa.EitaaSendResult;
import ir.omid.smartteacher.eitaa.internal.application.MessageCreditService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * پیاده‌سازی شبیه‌ساز ایتا — برای توسعه، آزمون، و اجرای بدون توکن.
 *
 * <p>با {@code app.mock.eitaa=true} فعال می‌شود.
 *
 * <p><b>🔴 چرا این کلاس امن‌تر از آن است که به نظر می‌رسد؟</b> چون در
 * همین حالت، {@link EitaaHttpClient} و {@link EitaaAppMessageAdapter} هم
 * ساخته <i>نمی‌شوند</i> — هر دو همان شرط را دارند. یعنی در حالت
 * شبیه‌ساز، هیچ مسیر کدی در برنامه وجود ندارد که بتواند به ایتای واقعی
 * درخواست بفرستد. اگر فقط این کلاس شرط داشت و آن دو نه، یک باگ
 * پیکربندی می‌توانست در حالت توسعه، پیام واقعی به گروه واقعی یک معلم
 * بفرستد.
 *
 * <p><b>⚠️ چرا {@code capabilities()} همه را برمی‌گرداند؟</b>
 * (سند {@code docs/eitaa.md} بخش ۸.۲) شبیه‌ساز دقیقاً برای این است که
 * توسعه‌دهنده بتواند همه مسیرها را بدون توکن و بدون اعتبار بیازماید.
 * تفاوتش با پیاده‌سازی واقعی این است که واقعی فقط چیزی را اعلام می‌کند
 * که توکنش موجود است.
 *
 * <p><b>⚠️ جعبه‌ی نامه (outbox) قابل بازرسی است.</b> این تنها راه
 * آزمودن «آیا اعلان درست ساخته شد؟» بدون فرستادن چیز واقعی است.
 * آزمون‌ها به‌جای بررسی لاگ، محتوای واقعی را می‌خوانند.
 */
@Component
@ConditionalOnProperty(name = "app.mock.eitaa", havingValue = "true")
public class MockEitaaAdapter implements EitaaAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockEitaaAdapter.class);

    /** نوع مقصد پیام — برای تفکیک در آزمون‌ها. */
    public enum Kind {
        /** مسیر الف — گروه یا کانال. */
        TEXT,
        /** مسیر الف — فایل. */
        FILE,
        /** مسیر ب — کاربر خصوصی. */
        PRIVATE
    }

    /**
     * یک پیام «فرستاده‌شده» در جعبه نامه.
     *
     * <p>توکن عمداً نگه داشته <b>نمی‌شود</b> — حتی در شبیه‌ساز، رازی که
     * لازم نیست، نگه داشته نمی‌شود. {@code BotTokenNeverLoggedTest} هم
     * روی همین اصل کار می‌کند.
     */
    public record SentMessage(
            Kind kind,
            String target,
            String text,
            String caption,
            String fileName,
            EitaaSendOptions options,
            String messageId,
            Instant sentAt) {

        @Override
        public String toString() {
            return "SentMessage{kind=" + kind + ", target=" + target
                    + ", messageId=" + messageId + '}';
        }
    }

    private static final String MOCK_BOT_USERNAME = "mock_eitaa_bot";

    private final List<SentMessage> outbox = new CopyOnWriteArrayList<>();
    private final AtomicLong messageIdSequence = new AtomicLong(1_000L);
    private final AtomicInteger remainingFailures = new AtomicInteger(0);
    private final MessageCreditService messageCredits;

    public MockEitaaAdapter(MessageCreditService messageCredits) {
        this.messageCredits = messageCredits;
    }

    private volatile String failureCode = "MOCK_FAILURE";
    private volatile String failureMessage = "شکست شبیه‌سازی‌شده برای آزمون";

    // ─────────────────────────── بازرسی جعبه نامه ───────────────────────────

    /** پیام‌های فرستاده‌شده، به ترتیب. */
    public List<SentMessage> outbox() {
        return List.copyOf(outbox);
    }

    /** آخرین پیام فرستاده‌شده. */
    public SentMessage lastMessage() {
        return outbox.isEmpty() ? null : outbox.get(outbox.size() - 1);
    }

    public void clearOutbox() {
        outbox.clear();
    }

    // ─────────────────────────── تزریق خطا ───────────────────────────

    /**
     * چند ارسال بعدی را شکست بدهد.
     *
     * <p>برای آزمونی مثل
     * {@code SendFailureDoesNotRollbackAssignmentTest} که باید ثابت کند
     * شکست ارسال، کار معلم را برنمی‌گرداند. بدون این سوئیچ، آن آزمون
     * نیاز به یک سرور ساختگی داشت که خطا بدهد — و آن‌وقت آزمون،
     * شبیه‌ساز را دور می‌زد و چیزی را می‌سنجید که در Production رخ
     * می‌دهد، نه آنچه توسعه‌دهنده می‌بیند.
     *
     * @param count          تعداد ارسال‌های بعدی که باید شکست بخورند
     * @param errorCode      کد خطا
     * @param errorMessage   پیام خطا
     */
    public void failNextSends(int count, String errorCode, String errorMessage) {
        this.failureCode = (errorCode == null) ? "MOCK_FAILURE" : errorCode;
        this.failureMessage = (errorMessage == null) ? "شکست شبیه‌سازی‌شده" : errorMessage;
        this.remainingFailures.set(Math.max(0, count));
    }

    /** برداشتن حالت خطا. */
    public void resetFailures() {
        remainingFailures.set(0);
    }

    /** آیا ارسال بعدی شکست می‌خورد؟ */
    private boolean shouldFail() {
        while (true) {
            int remaining = remainingFailures.get();
            if (remaining <= 0) {
                return false;
            }
            if (remainingFailures.compareAndSet(remaining, remaining - 1)) {
                return true;
            }
        }
    }

    private String nextMessageId() {
        return String.valueOf(messageIdSequence.getAndIncrement());
    }

    // ─────────────────────────── قابلیت‌ها ───────────────────────────

    @Override
    public Set<EitaaCapability> capabilities() {
        return EnumSet.allOf(EitaaCapability.class);
    }

    // ─────────────────────────── مسیر الف ───────────────────────────

    @Override
    public EitaaBotInfo getBotInfo(EitaaBotToken token) {
        return new EitaaBotInfo(1L, MOCK_BOT_USERNAME, "ربات", "شبیه‌سازی‌شده");
    }

    @Override
    public EitaaSendResult sendToChat(EitaaBotToken token,
                                      String chatId,
                                      String text,
                                      EitaaSendOptions options) {
        if (shouldFail()) {
            log.info("شبیه‌ساز: ارسال به {} به‌صورت عمدی شکست خورد", chatId);
            return EitaaSendResult.failure(failureCode, failureMessage);
        }

        EitaaSendOptions effectiveOptions =
                (options != null) ? options : EitaaSendOptions.defaults();
        String messageId = nextMessageId();

        outbox.add(new SentMessage(Kind.TEXT, chatId, text, null, null,
                effectiveOptions, messageId, Instant.now()));

        log.debug("شبیه‌ساز: پیام متنی به {} ثبت شد (شناسه {})", chatId, messageId);

        return EitaaSendResult.success(messageId, Instant.now());
    }

    @Override
    public EitaaSendResult sendFileToChat(EitaaBotToken token,
                                          String chatId,
                                          EitaaFilePayload file,
                                          String caption,
                                          EitaaSendOptions options) {
        if (shouldFail()) {
            log.info("شبیه‌ساز: ارسال فایل به {} به‌صورت عمدی شکست خورد", chatId);
            return EitaaSendResult.failure(failureCode, failureMessage);
        }

        if (file == null) {
            throw new IllegalArgumentException("فایل نباید خالی باشد");
        }

        EitaaSendOptions effectiveOptions =
                (options != null) ? options : EitaaSendOptions.defaults();
        String messageId = nextMessageId();

        outbox.add(new SentMessage(Kind.FILE, chatId, null, caption, file.fileName(),
                effectiveOptions, messageId, Instant.now()));

        log.debug("شبیه‌ساز: فایل {} به {} ثبت شد (شناسه {})",
                file.fileName(), chatId, messageId);

        return EitaaSendResult.success(messageId, Instant.now());
    }

    // ─────────────────────────── مسیر ب ───────────────────────────

    @Override
    public EitaaSendResult sendToUser(EitaaAppToken token, Long eitaaUserId, String text) {
        if (shouldFail()) {
            log.info("شبیه‌ساز: ارسال خصوصی به {} به‌صورت عمدی شکست خورد", eitaaUserId);
            return EitaaSendResult.failure(failureCode, failureMessage);
        }
        if (eitaaUserId == null) {
            throw new IllegalArgumentException("شناسه کاربر ایتا نباید خالی باشد");
        }

        // ⚠️ شناسه پیام null است — وفادار به مسیر ب واقعی، که
        // message_id برنمی‌گرداند. اگر شبیه‌ساز اینجا شناسه می‌ساخت،
        // آزمون‌ها روی چیزی سبز می‌شدند که در Production وجود ندارد.
        outbox.add(new SentMessage(Kind.PRIVATE, String.valueOf(eitaaUserId), text,
                null, null, EitaaSendOptions.defaults(), null, Instant.now()));

        log.debug("شبیه‌ساز: پیام خصوصی به {} ثبت شد", eitaaUserId);

        decrementCreditQuietly();
        return EitaaSendResult.success(null, Instant.now());
    }

    private void decrementCreditQuietly() {
        try {
            messageCredits.decrementOnSuccessfulUserSend();
        } catch (Exception ex) {
            log.debug("کسر اعتبار پیام نادیده گرفته شد: {}", ex.toString());
        }
    }

    @Override
    public EitaaSendResult notifyUser(Long eitaaUserId, EitaaNotification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("اعلان نباید خالی باشد");
        }
        return sendToUser(null, eitaaUserId, notification.text());
    }

    // ─────────────────────────── سلامت ───────────────────────────

    @Override
    public EitaaHealth checkHealth(EitaaBotToken token) {
        return EitaaHealth.up("ربات شبیه‌سازی‌شده", Instant.now());
    }
}
