package ir.omid.smartteacher.eitaa.internal.application;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.EitaaApiException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.eitaa.EitaaAdapter;
import ir.omid.smartteacher.eitaa.EitaaBotInfo;
import ir.omid.smartteacher.eitaa.EitaaCapabilityNotSupportedException;
import ir.omid.smartteacher.eitaa.EitaaProperties;
import ir.omid.smartteacher.eitaa.EitaaSendOptions;
import ir.omid.smartteacher.eitaa.EitaaSendResult;
import ir.omid.smartteacher.eitaa.internal.api.dto.AutomationRuleResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.ConnectClassRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateAutomationRuleRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaChannelRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaChannelResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaConnectionCheckResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaGroupResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.ScheduleMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.ScheduledMessageResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendChannelMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendGroupMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendMessageResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateAutomationRuleRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateEitaaChannelRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.domain.AutomationRule;
import ir.omid.smartteacher.eitaa.internal.domain.AutomationRuleRepository;
import ir.omid.smartteacher.eitaa.internal.domain.EitaaChannel;
import ir.omid.smartteacher.eitaa.internal.domain.EitaaChannelRepository;
import ir.omid.smartteacher.eitaa.internal.domain.EitaaGroup;
import ir.omid.smartteacher.eitaa.internal.domain.EitaaGroupRepository;
import ir.omid.smartteacher.eitaa.internal.domain.ScheduledMessage;
import ir.omid.smartteacher.eitaa.internal.domain.ScheduledMessageRepository;
import ir.omid.smartteacher.security.TenantGuard;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * کنترلر خروجی ایتا — فاز ۹ (گروه، کانال، زمان‌بندی، قواعد).
 */
@Service
public class EitaaOutboundService {

    private static final Logger log = LoggerFactory.getLogger(EitaaOutboundService.class);

    /**
     * 🔴 توکن اینجا پاس داده <b>نمی‌شود</b> — و این عمدی است.
     *
     * <p>نسخه پیشین یک ثابت با مقدار رشته‌ای {@code mock-outbound-token}
     * را به آداپتور می‌داد. آداپتور شبیه‌ساز آن را دور می‌انداخت، پس در
     * توسعه بی‌ضرر به نظر می‌رسید. ولی
     * {@code EitaaYarBotAdapter.resolveBotToken} توکن غیرnull را
     * <b>جدی</b> می‌گیرد و آن را داخل نشانی می‌گذارد:
     * {@code https://eitaayar.ir/api/mock-outbound-token/sendMessage}.
     *
     * <p>نتیجه یک باگ پروداکشنی <i>بی‌صدا</i> بود: با
     * {@code APP_MOCK_EITAA=false} و توکن ربات درست در {@code .env}، هر
     * ارسال به گروه و کانال با آن رشته ساختگی احراز هویت می‌شد و ایتا
     * ردش می‌کرد — به‌شکل {@code success=false}، نه استثنا. یعنی
     * «اتصال واقعی به کانال و گروه» هرگز واقعاً کار نمی‌کرد، بدون آنکه
     * چیزی در Log فریاد بزند.
     *
     * <p>پس {@code null} پاس داده می‌شود، چون قرارداد
     * {@code resolveBotToken} این است: توکن {@code null} یعنی «توکن
     * پیکربندی‌شده را بردار». و اگر هیچ توکنی پیکربندی نشده باشد،
     * {@code EitaaCapabilityNotSupportedException} پرتاب می‌شود —
     * یعنی «این کار ممکن نیست»، که دقیقاً همان چیزی است که
     * {@link ir.omid.smartteacher.eitaa.EitaaAdapter} مستند کرده است.
     * این بهتر از یک {@code success=false} مرموز است که علتش پیدا
     * نمی‌شود.
     */

    private static final Set<String> TRIGGERS = Set.of(
            "ASSIGNMENT_PUBLISHED",
            "SUBMISSION_EVALUATED",
            "EXAM_COMPLETED",
            "SCORE_BELOW_THRESHOLD",
            "SUBSCRIPTION_EXPIRING",
            "SCHEDULE");

    private static final Set<String> ACTIONS = Set.of(
            "AI_EVALUATE",
            "NOTIFY_TEACHER",
            "NOTIFY_STUDENT",
            "SEND_MESSAGE",
            "GENERATE_REPORT");

    /**
     * سقف طول {@code chat_id}.
     *
     * <p>🔴 این تنها قید عددی است که می‌گذاریم — و <b>عمداً هیچ الگوی
     * دقیقی اینجا نیست</b>. سند رسمی ایتا مجموعه‌نویسهٔ مجاز {@code chat_id}
     * را مشخص نمی‌کند، پس هر {@code regex}ِ تنگی «حدس» است و می‌تواند
     * شناسهٔ درست را رد کند. نسخهٔ پیشین این کلاس الگوی
     * {@code -?@?[A-Za-z0-9_]{3,100}} را داشت که {@code @group-1} — که
     * خودِ آزمون‌های همین پروژه می‌سازند — نامعتبر می‌خواند؛ یعنی
     * اعتبارسنجی‌ای که قرار بود از خطا جلوگیری کند، خودش خطا تولید
     * می‌کرد.
     */
    private static final int MAX_CHAT_ID_LENGTH = 100;

    /**
     * مقادیر مجاز {@code type} گروه — آینهٔ خودِ Schema، نه حدس:
     * {@code V17: CONSTRAINT chk_eitaa_groups_type CHECK (type IN ('GROUP', 'CHANNEL'))}.
     */
    private static final Set<String> GROUP_TYPES = Set.of("GROUP", "CHANNEL");

    /** {@code length} ستون {@code title} در {@code eitaa_groups}/{@code eitaa_channels}. */
    private static final int MAX_TITLE_LENGTH = 250;

    /** {@code length} ستون {@code username} در {@code eitaa_channels}. */
    private static final int MAX_USERNAME_LENGTH = 100;

    private final EitaaGroupRepository groupRepository;
    private final EitaaChannelRepository channelRepository;
    private final ScheduledMessageRepository scheduledMessageRepository;
    private final AutomationRuleRepository automationRuleRepository;
    private final EitaaAdapter eitaaAdapter;
    private final TenantGuard tenantGuard;
    private final EitaaProperties eitaaProperties;
    private final boolean mockMode;

    public EitaaOutboundService(EitaaGroupRepository groupRepository,
                                EitaaChannelRepository channelRepository,
                                ScheduledMessageRepository scheduledMessageRepository,
                                AutomationRuleRepository automationRuleRepository,
                                EitaaAdapter eitaaAdapter,
                                TenantGuard tenantGuard,
                                EitaaProperties eitaaProperties,
                                @Value("${app.mock.eitaa:false}") boolean mockMode) {
        this.groupRepository = groupRepository;
        this.channelRepository = channelRepository;
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.automationRuleRepository = automationRuleRepository;
        this.eitaaAdapter = eitaaAdapter;
        this.tenantGuard = tenantGuard;
        this.eitaaProperties = eitaaProperties;
        this.mockMode = mockMode;
    }

    // ─── گروه‌ها ───

    @Transactional(readOnly = true)
    public List<EitaaGroupResponse> listGroups() {
        return groupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(this::toGroupResponse)
                .toList();
    }

    @Transactional
    public EitaaGroupResponse createGroup(CreateEitaaGroupRequest request) {
        String chatId = request.eitaaChatId().trim();
        requireValidChatId(chatId, "گروه");
        requireMaxLength(trimToNull(request.title()), MAX_TITLE_LENGTH, "عنوان گروه");
        if (groupRepository.existsByEitaaChatIdAnyTenant(chatId)) {
            throw new DuplicateResourceException("گروه ایتا با این chat_id قبلاً ثبت شده است.");
        }
        EitaaGroup group = new EitaaGroup(chatId, trimToNull(request.title()));
        if (request.type() != null && !request.type().isBlank()) {
            String type = request.type().trim().toUpperCase(Locale.ROOT);
            requireValidGroupType(type, "گروه");
            group.setType(type);
        }
        try {
            return toGroupResponse(groupRepository.save(group));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("گروه ایتا با این chat_id قبلاً ثبت شده است.");
        }
    }

    @Transactional
    public EitaaGroupResponse connectClass(UUID groupId, ConnectClassRequest request) {
        EitaaGroup group = requireGroup(groupId);
        group.setClassroomId(request.classroomId());
        return toGroupResponse(group);
    }

    @Transactional
    public EitaaGroupResponse disconnectClass(UUID groupId) {
        EitaaGroup group = requireGroup(groupId);
        group.setClassroomId(null);
        return toGroupResponse(group);
    }

    /**
     * ویرایش جزئی گروه.
     *
     * <p>⚠️ سه‌حالته است، نه دوحالته — {@code null} یعنی «تغییر نده» و
     * رشتهٔ خالی برای {@code title} یعنی «پاک کن». اگر هر دو را یکسان
     * می‌دیدیم، کاربر نمی‌توانست عنوانی را که اشتباه وارد کرده پاک کند.
     */
    @Transactional
    public EitaaGroupResponse updateGroup(UUID groupId, UpdateEitaaGroupRequest request) {
        EitaaGroup group = requireGroup(groupId);

        String chatId = trimToNull(request.eitaaChatId());
        if (chatId != null && !chatId.equals(group.getEitaaChatId())) {
            // ⚠️ اعتبارسنجی فقط وقتی اجرا می‌شود که مقدار <b>واقعاً عوض
            // شده</b>. رکوردهای قدیمی ممکن است شناسه‌ای داشته باشند که
            // بررسی فعلی رد می‌کند؛ اگر بدون این شرط اعتبارسنجی می‌کردیم،
            // ویرایش «فقط عنوان» روی چنین رکوردی بی‌دلیل شکست می‌خورد.
            requireValidChatId(chatId, "گروه");
            if (groupRepository.existsByEitaaChatIdAnyTenantAndIdNot(chatId, groupId)) {
                throw new DuplicateResourceException("گروه ایتا با این chat_id قبلاً ثبت شده است.");
            }
            group.setEitaaChatId(chatId);
        }

        if (request.title() != null) {
            String title = trimToNull(request.title());
            requireMaxLength(title, MAX_TITLE_LENGTH, "عنوان گروه");
            group.setTitle(title);
        }
        if (request.type() != null) {
            String type = trimToNull(request.type());
            // ⚠️ عمداً با `title` فرق دارد: ستون `type` در پایگاه داده
            // `NOT NULL` است، پس «پاک‌کردن» برایش معنا ندارد و
            // `setType(null)` در زمان commit استثنای پایگاه داده می‌دهد.
            // اینجا رشتهٔ خالی یعنی «تغییر نده».
            if (type != null) {
                String normalized = type.toUpperCase(Locale.ROOT);
                requireValidGroupType(normalized, "گروه");
                group.setType(normalized);
            }
        }
        return toGroupResponse(group);
    }

    /** حذف نرم گروه — داده فیزیکی پاک نمی‌شود. */
    @Transactional
    public void deleteGroup(UUID groupId) {
        requireGroup(groupId).softDelete();
    }

    /**
     * اعتبارسنجی {@code chat_id} با خطای کاربرپسند.
     *
     * <p>پیام عمداً می‌گوید <b>چه چیزی</b> غلط است تا کاربر بداند اصلاح
     * کند؛ «شناسه نامعتبر است» به‌تنهایی کمکی نمی‌کند.
     *
     * <p>🔴 این متد را <b>هم</b> مسیر ساخت و <b>هم</b> مسیر ویرایش صدا
     * می‌زنند. نسخهٔ نخست فقط در ویرایش اعتبارسنجی می‌کرد، یعنی
     * {@code POST} با {@code "my group"} یا یک لینک پذیرفته می‌شد ولی
     * همان مقدار در {@code PUT} رد می‌شد — یک عدم‌تقارن که کاربر را
     * سردرگم می‌کند و از آزمون‌ها هم رد می‌شود.
     */
    private static void requireValidChatId(String chatId, String target) {
        if (isValidChatId(chatId)) {
            return;
        }
        if (chatId.length() > MAX_CHAT_ID_LENGTH) {
            throw new AppException(
                    ErrorCode.VALIDATION_FAILED,
                    "شناسه چت " + target + " بیش از حد بلند است (حداکثر "
                            + MAX_CHAT_ID_LENGTH + " نویسه).");
        }
        throw new AppException(
                ErrorCode.VALIDATION_FAILED,
                "شناسه چت " + target + " نامعتبر است. شناسه را بدون @ و بدون لینک وارد کنید "
                        + "(مثلاً my_group یا 11268746).");
    }

    /**
     * اعتبارسنجی {@code type} گروه.
     *
     * <p>مقادیر مجاز از <b>خودِ Schema</b> خوانده شده‌اند، نه حدس:
     * {@code V17: CONSTRAINT chk_eitaa_groups_type CHECK (type IN ('GROUP',
     * 'CHANNEL'))}. بدون این بررسی، {@code "FOO"} تا {@code commit} می‌رفت
     * و آنجا به‌شکل نقض محدودیت بیرون می‌آمد؛ {@code GlobalExceptionHandler}
     * آن را {@code RESOURCE_CONFLICT} (۴۰۹) با پیام «تناقض وضعیت» می‌کرد —
     * یعنی خطای <i>ورودی</i> کاربر با پیام خطای <i>سیستم</i> گزارش می‌شد.
     */
    private static void requireValidGroupType(String type, String target) {
        if (!GROUP_TYPES.contains(type)) {
            throw new AppException(
                    ErrorCode.VALIDATION_FAILED,
                    "نوع " + target + " نامعتبر است. مقادیر مجاز: "
                            + String.join(" یا ", GROUP_TYPES) + ".");
        }
    }

    /** سقف طول — آینهٔ {@code length} ستون در پایگاه داده. */
    private static void requireMaxLength(String value, int max, String fieldLabel) {
        if (value != null && value.length() > max) {
            throw new AppException(
                    ErrorCode.VALIDATION_FAILED,
                    fieldLabel + " بیش از حد بلند است (حداکثر " + max + " نویسه).");
        }
    }

    @Transactional
    public SendMessageResponse sendMessage(UUID groupId, SendGroupMessageRequest request) {
        EitaaGroup group = requireGroup(groupId);
        if (!group.isCanSendMessages()) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "ارسال به این گروه غیرفعال است.");
        }
        EitaaSendResult result = eitaaAdapter.sendToChat(
                null, group.getEitaaChatId(), request.text().trim(),
                toSendOptions(request.pin(), request.viewCountForDelete(), null));
        return new SendMessageResponse(
                result.success(), result.messageId(), result.errorCode(), result.errorMessage());
    }

    // ─── کانال‌ها ───

    @Transactional(readOnly = true)
    public List<EitaaChannelResponse> listChannels() {
        return channelRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(this::toChannelResponse)
                .toList();
    }

    @Transactional
    public EitaaChannelResponse createChannel(CreateEitaaChannelRequest request) {
        String chatId = request.eitaaChatId().trim();
        requireValidChatId(chatId, "کانال");
        requireMaxLength(trimToNull(request.title()), MAX_TITLE_LENGTH, "عنوان کانال");
        requireMaxLength(trimToNull(request.username()), MAX_USERNAME_LENGTH, "نام کاربری کانال");
        if (channelRepository.existsByEitaaChatIdAnyTenant(chatId)) {
            throw new DuplicateResourceException("کانال ایتا با این chat_id قبلاً ثبت شده است.");
        }
        EitaaChannel channel = new EitaaChannel(chatId, trimToNull(request.title()));
        channel.setUsername(trimToNull(request.username()));
        try {
            return toChannelResponse(channelRepository.save(channel));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("کانال ایتا با این chat_id قبلاً ثبت شده است.");
        }
    }

    @Transactional
    public SendMessageResponse sendToChannel(UUID channelId, SendChannelMessageRequest request) {
        EitaaChannel channel = requireChannel(channelId);
        if (!channel.isCanPost()) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "ارسال به این کانال غیرفعال است.");
        }
        EitaaSendResult result = eitaaAdapter.sendToChat(
                null, channel.getEitaaChatId(), request.text().trim(),
                toSendOptions(request.pin(), request.viewCountForDelete(), null));
        return new SendMessageResponse(
                result.success(), result.messageId(), result.errorCode(), result.errorMessage());
    }

    /** ویرایش جزئی کانال — همان قرارداد سه‌حالتهٔ {@link #updateGroup}. */
    @Transactional
    public EitaaChannelResponse updateChannel(UUID channelId, UpdateEitaaChannelRequest request) {
        EitaaChannel channel = requireChannel(channelId);

        String chatId = trimToNull(request.eitaaChatId());
        if (chatId != null && !chatId.equals(channel.getEitaaChatId())) {
            requireValidChatId(chatId, "کانال");
            if (channelRepository.existsByEitaaChatIdAnyTenantAndIdNot(chatId, channelId)) {
                throw new DuplicateResourceException("کانال ایتا با این chat_id قبلاً ثبت شده است.");
            }
            channel.setEitaaChatId(chatId);
        }

        if (request.title() != null) {
            String title = trimToNull(request.title());
            requireMaxLength(title, MAX_TITLE_LENGTH, "عنوان کانال");
            channel.setTitle(title);
        }
        if (request.username() != null) {
            String username = trimToNull(request.username());
            requireMaxLength(username, MAX_USERNAME_LENGTH, "نام کاربری کانال");
            channel.setUsername(username);
        }
        return toChannelResponse(channel);
    }

    /** حذف نرم کانال — داده فیزیکی پاک نمی‌شود. */
    @Transactional
    public void deleteChannel(UUID channelId) {
        requireChannel(channelId).softDelete();
    }

    // ─── بررسی اتصال ───

    /**
     * بررسی سلامت ربات — بدون هیچ گفت‌وگوی مشخص.
     *
     * <p>⚠️ درباره ترتیب: اول توکن از پیکربندی خوانده می‌شود. اگر توکن
     * نباشد و حالت شبیه‌ساز هم خاموش باشد، همان‌جا گزارش می‌کنیم و
     * {@code getMe} را صدا نمی‌زنیم — چون آداپتور در آن حالت
     * {@code EitaaCapabilityNotSupportedException} پرتاب می‌کند که
     * پیامش («قابلیت … وجود ندارد») مشکل واقعی («توکن تنظیم نشده») را
     * گمراه‌کننده روایت می‌کند.
     */
    @Transactional(readOnly = true)
    public EitaaConnectionCheckResponse health() {
        return probe(null);
    }

    /** بررسی اتصال یک کانال ثبت‌شده. */
    @Transactional(readOnly = true)
    public EitaaConnectionCheckResponse verifyChannel(UUID channelId) {
        return probe(requireChannel(channelId).getEitaaChatId());
    }

    /** بررسی اتصال یک گروه ثبت‌شده. */
    @Transactional(readOnly = true)
    public EitaaConnectionCheckResponse verifyGroup(UUID groupId) {
        return probe(requireGroup(groupId).getEitaaChatId());
    }

    private EitaaConnectionCheckResponse probe(String chatId) {
        Instant now = Instant.now();
        boolean tokenConfigured = eitaaProperties.hasBotToken();

        String username = null;
        String displayName = null;
        boolean healthy;
        String detail;

        if (!tokenConfigured && !mockMode) {
            healthy = false;
            detail = "توکن ربات پیکربندی نشده است. APP_EITAA_BOT_TOKEN را تنظیم کنید.";
        } else {
            try {
                // ⚠️ توکن null یعنی «توکن پیکربندی‌شده را بردار» — نه
                // «توکنی نیست». دلیلش در بالای همین کلاس توضیح داده شده.
                EitaaBotInfo info = eitaaAdapter.getBotInfo(null);
                healthy = true;
                username = info.username();
                displayName = info.displayName();
                detail = "ربات پاسخ داد.";
            } catch (EitaaCapabilityNotSupportedException ex) {
                healthy = false;
                detail = "این آداپتور ایتا از getMe پشتیبانی نمی‌کند.";
            } catch (EitaaApiException ex) {
                // ⚠️ پیام EitaaApiException عمداً متن خام ایتا را حمل
                // نمی‌کند، پس نمایشش به کاربر امن است. متن خام فقط در
                // technicalDetail است و هرگز اینجا نمی‌آید.
                healthy = false;
                detail = ex.getMessage();
                log.warn("بررسی اتصال ایتا ناموفق بود: کد={}", ex.remoteErrorCode());
            } catch (RuntimeException ex) {
                healthy = false;
                detail = "خطای غیرمنتظره در بررسی اتصال با ایتا.";
                log.warn("بررسی اتصال ایتا با خطای غیرمنتظره شکست خورد", ex);
            }
        }

        if (mockMode) {
            // 🔴 این شاخه حیاتی است، نه تزئینی: در حالت شبیه‌ساز getMe
            // همیشه موفق است. اگر این را نگوییم، کاربر «سالم» می‌بیند و
            // خیال می‌کند اتصال واقعی تأیید شد — در حالی که هیچ بسته‌ای
            // به ایتا نرفته است.
            detail = "حالت شبیه‌ساز فعال است (app.mock.eitaa=true) — "
                    + "این نتیجه اتصال واقعی به ایتا را تأیید نمی‌کند.";
        }

        return new EitaaConnectionCheckResponse(
                tokenConfigured, mockMode, healthy, username, displayName,
                chatId, chatId == null || isValidChatId(chatId), detail, now);
    }

    /**
     * بررسی سرانگشتی قالب {@code chat_id}.
     *
     * <p>🔴 شناسه <b>بازنویسی نمی‌شود</b> — و این عمدی است. حذف
     * {@code @} از {@code @channel} می‌تواند یک شناسه معتبر را به
     * شناسه‌ای نامعتبر تبدیل کند. ما فقط نگاه می‌کنیم، دست نمی‌زنیم.
     *
     * <p>چهار چیز رد می‌شود، و همه‌شان «قطعاً غلط»اند نه «احتمالاً غلط»:
     * خالی · بلندتر از {@value #MAX_CHAT_ID_LENGTH} · دارای فاصلهٔ داخلی
     * (چون هیچ شناسهٔ چتی فاصله ندارد و فاصله تقریباً همیشه یعنی کاربر
     * عنوان را جای شناسه چسبانده) · {@code http://} یا {@code https://}
     * (یعنی کاربر لینک را جای شناسه داده).
     */
    static boolean isValidChatId(String chatId) {
        if (chatId == null) {
            return false;
        }
        String value = chatId.trim();
        if (value.isEmpty() || value.length() > MAX_CHAT_ID_LENGTH) {
            return false;
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return !lower.startsWith("http://") && !lower.startsWith("https://");
    }

    // ─── زمان‌بندی ───

    /**
     * ذخیره به‌صورت PENDING؛ جاب زمان‌بندی ارسال می‌کند
     * (با پارامتر بومی {@code date} به‌عنوان لایه دوم).
     */
    @Transactional
    public ScheduledMessageResponse scheduleMessage(ScheduleMessageRequest request) {
        EitaaGroup group = requireGroup(request.groupId());
        ScheduledMessage scheduled = new ScheduledMessage(
                "GROUP", group.getId(), request.text().trim(), request.scheduledAt());
        scheduled.setStatus("PENDING");
        scheduled = scheduledMessageRepository.save(scheduled);
        return toScheduledResponse(scheduled);
    }

    /**
     * پردازش پیام‌های سررسید — برای جاب و تست.
     *
     * @return تعداد پیام‌های پردازش‌شده
     */
    @Transactional
    public int processDueScheduledMessages(Instant now) {
        List<ScheduledMessage> due = scheduledMessageRepository.findDuePending(now);
        int processed = 0;
        for (ScheduledMessage msg : due) {
            processOneScheduled(msg);
            processed++;
        }
        return processed;
    }

    private void processOneScheduled(ScheduledMessage msg) {
        String chatId = resolveChatId(msg);
        if (chatId == null) {
            msg.setStatus("FAILED");
            msg.setErrorMessage("مقصد یافت نشد");
            msg.setAttempts(msg.getAttempts() + 1);
            msg.setLastAttemptAt(Instant.now());
            return;
        }

        EitaaSendResult result = eitaaAdapter.sendToChat(
                null,
                chatId,
                msg.getContentText(),
                EitaaSendOptions.scheduledAt(msg.getScheduledAt()));

        msg.setAttempts(msg.getAttempts() + 1);
        msg.setLastAttemptAt(Instant.now());
        if (result.success()) {
            msg.setStatus("SENT");
            msg.setEitaaMessageId(result.messageId());
            msg.setErrorMessage(null);
        } else {
            msg.setStatus("FAILED");
            msg.setErrorMessage(result.errorMessage());
            log.warn("ارسال زمان‌بندی‌شده {} شکست خورد: {}", msg.getId(), result.errorCode());
        }
    }

    private String resolveChatId(ScheduledMessage msg) {
        if ("GROUP".equals(msg.getTargetType()) || "CHANNEL".equals(msg.getTargetType())) {
            if ("GROUP".equals(msg.getTargetType())) {
                return groupRepository.findByIdAndDeletedAtIsNull(msg.getTargetId())
                        .map(EitaaGroup::getEitaaChatId)
                        .orElse(null);
            }
            return channelRepository.findByIdAndDeletedAtIsNull(msg.getTargetId())
                    .map(EitaaChannel::getEitaaChatId)
                    .orElse(null);
        }
        return null;
    }

    // ─── قواعد خودکارسازی ───

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> listAutomationRules() {
        return automationRuleRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(this::toRuleResponse)
                .toList();
    }

    @Transactional
    public AutomationRuleResponse createAutomationRule(CreateAutomationRuleRequest request) {
        String trigger = request.triggerType().trim().toUpperCase();
        String action = request.actionType().trim().toUpperCase();
        if (!TRIGGERS.contains(trigger)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "نوع تریگر نامعتبر است.");
        }
        if (!ACTIONS.contains(action)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "نوع اقدام نامعتبر است.");
        }
        AutomationRule rule = new AutomationRule(request.name().trim(), trigger, action);
        rule.setTriggerConfig(trimToNull(request.triggerConfig()));
        rule.setActionConfig(trimToNull(request.actionConfig()));
        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }
        return toRuleResponse(automationRuleRepository.save(rule));
    }

    @Transactional
    public AutomationRuleResponse updateAutomationRule(UUID id, UpdateAutomationRuleRequest request) {
        AutomationRule rule = requireRule(id);
        if (request.name() != null && !request.name().isBlank()) {
            rule.setName(request.name().trim());
        }
        if (request.triggerConfig() != null) {
            rule.setTriggerConfig(trimToNull(request.triggerConfig()));
        }
        if (request.actionType() != null && !request.actionType().isBlank()) {
            String action = request.actionType().trim().toUpperCase();
            if (!ACTIONS.contains(action)) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "نوع اقدام نامعتبر است.");
            }
            rule.setActionType(action);
        }
        if (request.actionConfig() != null) {
            rule.setActionConfig(trimToNull(request.actionConfig()));
        }
        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }
        return toRuleResponse(rule);
    }

    @Transactional
    public void deleteAutomationRule(UUID id) {
        requireRule(id).softDelete();
    }

    /**
     * اجرای قواعد فعال برای یک تریگر — از موتور رویداد فراخوانی می‌شود.
     *
     * <p>⚠️ باید داخل {@code TenantContext} مستأجر مربوطه صدا زده شود.
     */
    @Transactional
    public int executeRulesForTrigger(String triggerType, String messageText) {
        return executeRulesForTrigger(triggerType, messageText, AutomationTriggerContext.NONE);
    }

    /**
     * اجرای قواعد فعال — با زمینه اختیاری برای فیلتر (مثلاً آستانه نمره).
     */
    @Transactional
    public int executeRulesForTrigger(String triggerType,
                                      String messageText,
                                      AutomationTriggerContext context) {
        List<AutomationRule> rules =
                automationRuleRepository.findByTriggerTypeAndEnabledTrueAndDeletedAtIsNull(triggerType);
        int executed = 0;
        for (AutomationRule rule : rules) {
            if (!matchesTrigger(rule, context)) {
                continue;
            }
            executeRule(rule, messageText);
            executed++;
        }
        return executed;
    }

    private boolean matchesTrigger(AutomationRule rule, AutomationTriggerContext context) {
        if (!"SCORE_BELOW_THRESHOLD".equals(rule.getTriggerType())) {
            return true;
        }
        if (context == null || context.scorePercent() == null) {
            return false;
        }
        BigDecimal threshold = parseThresholdPercent(rule.getTriggerConfig());
        return context.scorePercent().compareTo(threshold) < 0;
    }

    static BigDecimal parseThresholdPercent(String triggerConfig) {
        if (triggerConfig == null || triggerConfig.isBlank()) {
            return new BigDecimal("50");
        }
        String cfg = triggerConfig.trim();
        int key = cfg.indexOf("thresholdPercent");
        if (key < 0) {
            return new BigDecimal("50");
        }
        int colon = cfg.indexOf(':', key);
        if (colon < 0) {
            return new BigDecimal("50");
        }
        String rest = cfg.substring(colon + 1).trim();
        int end = rest.indexOf(',');
        if (end < 0) {
            end = rest.indexOf('}');
        }
        if (end > 0) {
            rest = rest.substring(0, end);
        }
        rest = rest.replace("\"", "").trim();
        try {
            return new BigDecimal(rest);
        } catch (NumberFormatException ex) {
            return new BigDecimal("50");
        }
    }

    private void executeRule(AutomationRule rule, String messageText) {
        String text = messageText != null ? messageText : ("رویداد: " + rule.getTriggerType());
        if ("NOTIFY_TEACHER".equals(rule.getActionType())
                || "SEND_MESSAGE".equals(rule.getActionType())
                || "NOTIFY_STUDENT".equals(rule.getActionType())) {
            String chatId = extractChatId(rule.getActionConfig());
            if (chatId == null) {
                // بدون مقصد مشخص — به اولین گروه فعال بفرست (Mock)
                chatId = groupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                        .findFirst()
                        .map(EitaaGroup::getEitaaChatId)
                        .orElse("automation-fallback");
            }
            eitaaAdapter.sendToChat(null, chatId, text, EitaaSendOptions.defaults());
        }
        rule.markExecuted();
        automationRuleRepository.save(rule);
    }

    private static String extractChatId(String actionConfig) {
        if (actionConfig == null || actionConfig.isBlank()) {
            return null;
        }
        // ساده: {"chatId":"..."} یا مقدار خام chatId
        String cfg = actionConfig.trim();
        int key = cfg.indexOf("\"chatId\"");
        if (key < 0) {
            key = cfg.indexOf("chatId");
        }
        if (key >= 0) {
            int colon = cfg.indexOf(':', key);
            if (colon > 0) {
                String rest = cfg.substring(colon + 1).trim();
                if (rest.startsWith("\"")) {
                    int end = rest.indexOf('"', 1);
                    if (end > 1) {
                        return rest.substring(1, end);
                    }
                }
            }
        }
        if (!cfg.startsWith("{")) {
            return cfg;
        }
        return null;
    }

    // ─── کمکی ───

    private EitaaGroup requireGroup(UUID id) {
        EitaaGroup group = groupRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        return tenantGuard.requireOwned(group, "EitaaGroup");
    }

    private EitaaChannel requireChannel(UUID id) {
        EitaaChannel channel = channelRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        return tenantGuard.requireOwned(channel, "EitaaChannel");
    }

    private AutomationRule requireRule(UUID id) {
        AutomationRule rule = automationRuleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(ResourceNotFoundException::generic);
        return tenantGuard.requireOwned(rule, "AutomationRule");
    }

    static EitaaSendOptions toSendOptions(Boolean pin, Integer viewCountForDelete, Instant scheduledAt) {
        boolean pinAfter = Boolean.TRUE.equals(pin);
        return new EitaaSendOptions(false, null, scheduledAt, pinAfter, viewCountForDelete);
    }

    private EitaaGroupResponse toGroupResponse(EitaaGroup group) {
        return new EitaaGroupResponse(
                group.getId(), group.getEitaaChatId(), group.getTitle(), group.getType(),
                group.getClassroomId(), group.getStatus(), group.isCanSendMessages(),
                group.getCreatedAt());
    }

    private EitaaChannelResponse toChannelResponse(EitaaChannel channel) {
        return new EitaaChannelResponse(
                channel.getId(), channel.getEitaaChatId(), channel.getTitle(),
                channel.getUsername(), channel.getStatus(), channel.isCanPost(),
                channel.getCreatedAt());
    }

    private ScheduledMessageResponse toScheduledResponse(ScheduledMessage msg) {
        return new ScheduledMessageResponse(
                msg.getId(), msg.getTargetType(), msg.getTargetId(), msg.getContentText(),
                msg.getScheduledAt(), msg.getStatus(), msg.getEitaaMessageId());
    }

    private AutomationRuleResponse toRuleResponse(AutomationRule rule) {
        return new AutomationRuleResponse(
                rule.getId(), rule.getName(), rule.getTriggerType(), rule.getTriggerConfig(),
                rule.getActionType(), rule.getActionConfig(), rule.isEnabled(),
                rule.getExecutionCount(), rule.getLastExecutedAt(), rule.getCreatedAt());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
