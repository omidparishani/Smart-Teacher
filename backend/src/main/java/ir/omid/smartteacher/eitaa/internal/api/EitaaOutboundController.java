package ir.omid.smartteacher.eitaa.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.eitaa.internal.api.dto.AutomationRuleResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.ChannelPublishQueueResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.ConnectClassRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateAutomationRuleRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaChannelRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaChannelResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaConnectionCheckResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaGroupResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.EnqueueChannelPublishRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.MessageCreditResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.ScheduleMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.ScheduledMessageResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendChannelMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendGroupMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendMessageResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateAutomationRuleRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateEitaaChannelRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.UpdateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.application.ChannelPublishQueueService;
import ir.omid.smartteacher.eitaa.internal.application.EitaaOutboundService;
import ir.omid.smartteacher.eitaa.internal.application.MessageCreditService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مسیرهای خروجی ایتا — {@code /api/v1/eitaa/**} مطابق docs/api.md بخش ۴.۸.
 *
 * <p>❌ هیچ webhook. ArchUnit_noWebhookControllerTest سبز می‌ماند.
 */
@RestController
@RequestMapping("/api/v1/eitaa")
@Tag(name = "ایتا خروجی", description = "گروه، کانال، ارسال، زمان‌بندی و قواعد — فقط خروجی")
public class EitaaOutboundController {

    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER')";

    private final EitaaOutboundService outboundService;
    private final MessageCreditService messageCreditService;
    private final ChannelPublishQueueService channelPublishQueueService;

    public EitaaOutboundController(EitaaOutboundService outboundService,
                                   MessageCreditService messageCreditService,
                                   ChannelPublishQueueService channelPublishQueueService) {
        this.outboundService = outboundService;
        this.messageCreditService = messageCreditService;
        this.channelPublishQueueService = channelPublishQueueService;
    }

    @GetMapping("/health")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "بررسی سلامت اتصال ربات ایتا",
            description = "با getMe — هیچ پیامی فرستاده نمی‌شود. "
                    + "⚠️ «سالم» یعنی توکن معتبر است، نه اینکه ارسال به یک "
                    + "گفت‌وگوی مشخص کار می‌کند؛ ایتا متدی برای بررسی وجود "
                    + "یا دسترسی گفت‌وگو ندارد.")
    public EitaaConnectionCheckResponse health() {
        return outboundService.health();
    }

    @GetMapping("/groups")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست گروه‌های ایتا")
    public List<EitaaGroupResponse> listGroups() {
        return outboundService.listGroups();
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ثبت گروه ایتا")
    public EitaaGroupResponse createGroup(@Valid @RequestBody CreateEitaaGroupRequest request) {
        return outboundService.createGroup(request);
    }

    @PostMapping("/groups/{id}/connect-class")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "اتصال گروه به کلاس")
    public EitaaGroupResponse connectClass(
            @PathVariable UUID id,
            @Valid @RequestBody ConnectClassRequest request) {
        return outboundService.connectClass(id, request);
    }

    @DeleteMapping("/groups/{id}/connect-class")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "قطع اتصال گروه از کلاس")
    public EitaaGroupResponse disconnectClass(@PathVariable UUID id) {
        return outboundService.disconnectClass(id);
    }

    @PutMapping("/groups/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش گروه (به‌روزرسانی جزئی)",
            description = "null یعنی «تغییر نده». برای title رشتهٔ خالی یعنی «پاک کن»؛ "
                    + "برای type رشتهٔ خالی یعنی «تغییر نده» چون ستون NOT NULL است. "
                    + "chat_id فقط اگر غیرخالی و متفاوت باشد اعتبارسنجی و جایگزین می‌شود.")
    // ⚠️ عمداً `@Valid` ندارد: `UpdateEitaaGroupRequest` هیچ قید Bean
    // Validation ندارد (قرارداد سه‌حالتهٔ null/خالی با annotationهای
    // استاندارد بیان‌شدنی نیست)، پس `@Valid` فقط این توهم را می‌ساخت که
    // بدنه در مرز اعتبارسنجی می‌شود. اعتبارسنجی واقعی در سرویس است و
    // همان ۴۰۰ (VALIDATION_FAILED) با پیام فارسی می‌دهد.
    public EitaaGroupResponse updateGroup(
            @PathVariable UUID id,
            @RequestBody UpdateEitaaGroupRequest request) {
        return outboundService.updateGroup(id, request);
    }

    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف نرم گروه",
            description = "⚠️ حذف نرم است (deleted_at) — رکورد فیزیکی پاک نمی‌شود "
                    + "و از فهرست‌ها ناپدید می‌شود.")
    public void deleteGroup(@PathVariable UUID id) {
        outboundService.deleteGroup(id);
    }

    @PostMapping("/groups/{id}/verify")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "بررسی اتصال گروه",
            description = "اعتبار توکن و قالب chat_id را بررسی می‌کند. "
                    + "ایتا متدی برای تأیید وجود یا دسترسی گروه ندارد؛ "
                    + "برای اطمینان قطعی یک پیام آزمایشی بفرستید.")
    public EitaaConnectionCheckResponse verifyGroup(@PathVariable UUID id) {
        return outboundService.verifyGroup(id);
    }

    @PostMapping("/groups/{id}/send-message")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ارسال فوری پیام به گروه (pin / viewCountForDelete)")
    public SendMessageResponse sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendGroupMessageRequest request) {
        return outboundService.sendMessage(id, request);
    }

    @GetMapping("/channels")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست کانال‌ها")
    public List<EitaaChannelResponse> listChannels() {
        return outboundService.listChannels();
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ثبت کانال ایتا")
    public EitaaChannelResponse createChannel(@Valid @RequestBody CreateEitaaChannelRequest request) {
        return outboundService.createChannel(request);
    }

    @PutMapping("/channels/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش کانال (به‌روزرسانی جزئی)",
            description = "null یعنی «تغییر نده»؛ رشتهٔ خالی برای title یا username "
                    + "یعنی «پاک کن». chat_id فقط اگر غیرخالی و متفاوت باشد "
                    + "اعتبارسنجی و جایگزین می‌شود.")
    // ⚠️ عمداً `@Valid` ندارد — دلیلش روی `updateGroup` نوشته شده است.
    public EitaaChannelResponse updateChannel(
            @PathVariable UUID id,
            @RequestBody UpdateEitaaChannelRequest request) {
        return outboundService.updateChannel(id, request);
    }

    @DeleteMapping("/channels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف نرم کانال",
            description = "⚠️ حذف نرم است (deleted_at) — رکورد فیزیکی پاک نمی‌شود.")
    public void deleteChannel(@PathVariable UUID id) {
        outboundService.deleteChannel(id);
    }

    @PostMapping("/channels/{id}/verify")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "بررسی اتصال کانال",
            description = "اعتبار توکن و قالب chat_id را بررسی می‌کند. "
                    + "ایتا متدی برای تأیید وجود یا دسترسی کانال ندارد؛ "
                    + "برای اطمینان قطعی یک پیام آزمایشی بفرستید.")
    public EitaaConnectionCheckResponse verifyChannel(@PathVariable UUID id) {
        return outboundService.verifyChannel(id);
    }

    @PostMapping("/channels/{id}/send-message")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ارسال به کانال")
    public SendMessageResponse sendChannelMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendChannelMessageRequest request) {
        return outboundService.sendToChannel(id, request);
    }

    @PostMapping("/channels/{id}/publish-queue")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "صف انتشار کانال — تولید متن با Mock-AI")
    public ChannelPublishQueueResponse enqueueChannelPublish(
            @PathVariable UUID id,
            @Valid @RequestBody EnqueueChannelPublishRequest request) {
        return channelPublishQueueService.enqueue(id, request.topic());
    }

    @GetMapping("/message-credits")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "مانده اعتبار پیام مسیر برنامه")
    public MessageCreditResponse messageCredits() {
        return messageCreditService.currentBalance();
    }

    @PostMapping("/scheduled-messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "زمان‌بندی پیام (PENDING تا جاب ارسال کند)")
    public ScheduledMessageResponse schedule(
            @Valid @RequestBody ScheduleMessageRequest request) {
        return outboundService.scheduleMessage(request);
    }

    @GetMapping("/automation-rules")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست قواعد خودکارسازی")
    public List<AutomationRuleResponse> listAutomationRules() {
        return outboundService.listAutomationRules();
    }

    @PostMapping("/automation-rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت قاعده خودکارسازی")
    public AutomationRuleResponse createAutomationRule(
            @Valid @RequestBody CreateAutomationRuleRequest request) {
        return outboundService.createAutomationRule(request);
    }

    @PutMapping("/automation-rules/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش قاعده")
    public AutomationRuleResponse updateAutomationRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAutomationRuleRequest request) {
        return outboundService.updateAutomationRule(id, request);
    }

    @DeleteMapping("/automation-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف نرم قاعده")
    public void deleteAutomationRule(@PathVariable UUID id) {
        outboundService.deleteAutomationRule(id);
    }
}
