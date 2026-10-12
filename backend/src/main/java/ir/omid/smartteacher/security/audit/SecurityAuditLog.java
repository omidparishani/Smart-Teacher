package ir.omid.smartteacher.security.audit;

import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.audit.AuditLog;
import ir.omid.smartteacher.audit.AuditLogRepository;
import ir.omid.smartteacher.audit.Severity;
import ir.omid.smartteacher.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
// ⚠️ Jackson 3 است، نه Jackson 2. در Spring Boot 4 پیکربندی خودکار جکسون
// به ماژول spring-boot-jackson منتقل شده و آن ماژول Jackson 3
// (tools.jackson) را سیم‌کشی می‌کند؛ نسخه‌ی com.fasterxml.jackson هیچ
// Beanی ندارد و بالا آمدن کانتکست با NoSuchBeanDefinitionException
// می‌شکند. Jackson 3 استثنای unchecked پرتاب می‌کند (JacksonException از
// RuntimeException ارث می‌برد)، پس catch (Exception) پایین‌تر هنوز کار
// می‌کند.
import tools.jackson.databind.ObjectMapper;

/**
 * ثبت رویدادهای امنیتی.
 *
 * <p><b>دو کانال ثبت، به‌عمد:</b>
 * <ol>
 *   <li><b>Log</b> — همیشه و بی‌قید‌و‌شرط، پیش از هر کار دیگری. این
 *       کانال هرگز با بازگشت تراکنش پاک نمی‌شود.</li>
 *   <li><b>جدول {@code audit_logs}</b> — برای پرس‌وجو، داشبورد و
 *       همبستگی با سایر رویدادها.</li>
 * </ol>
 *
 * <p><b>⚠️ چرا کانال دوم در تراکنش جدید ({@code REQUIRES_NEW})؟</b>
 * چون رویدادهای امنیتی معمولاً <i>پیش از</i> شکست عملیات ثبت می‌شوند:
 * {@code TenantGuard} رویداد را ثبت می‌کند و بعد استثنا پرتاب می‌کند.
 * اگر ثبت در همان تراکنش بود، بازگشت تراکنش آن را هم پاک می‌کرد و
 * دقیقاً مهم‌ترین رویدادها هرگز در دیتابیس نمی‌ماندند.
 *
 * <p><b>⚠️ چرا خطای ثبت دیتابیس، درخواست را نمی‌شکند؟</b> چون کانال
 * اول از قبل نوشته شده است. اگر دیتابیس قفل باشد یا پر شده باشد،
 * شکستاندن درخواست کاربر هیچ سود امنیتی ندارد؛ فقط دسترس‌پذیری را
 * پایین می‌آورد. پس شکست کانال دوم با درجه ERROR در Log می‌نشیند و
 * جریان ادامه پیدا می‌کند.
 */
@Component
public class SecurityAuditLog {

    /**
     * Logger اختصاصی امنیت.
     *
     * <p>جدا بودنش عمدی است: می‌شود در پیکربندی Logback همین نام را به
     * یک فایل جداگانه و با نگهداری طولانی‌تر فرستاد، بی‌آنکه رویدادهای
     * امنیتی در میان لاگ‌های عادی گم شوند.
     */
    private static final Logger SECURITY = LoggerFactory.getLogger("SECURITY");

    private static final String MDC_TRACE_ID = "traceId";

    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final int MAX_TRACE_ID_LENGTH = 64;

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiresNewTransaction;

    public SecurityAuditLog(AuditLogRepository repository,
                            ObjectMapper objectMapper,
                            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * ثبت یک رویداد امنیتی.
     *
     * @param event       نوع رویداد
     * @param tenantId    مستأجر مربوطه؛ {@code null} اگر نامعلوم است
     * @param actorUserId کاربر مربوطه؛ {@code null} اگر نامعلوم است
     * @param actorType   منشأ رویداد
     * @param entityType  نوع موجودیت مرتبط؛ می‌تواند {@code null} باشد
     * @param entityId    شناسه موجودیت مرتبط؛ می‌تواند {@code null} باشد
     * @param metadata    اطلاعات تکمیلی — <b>نباید</b> راز داشته باشد
     */
    public void record(SecurityEvent event,
                       UUID tenantId,
                       UUID actorUserId,
                       ActorType actorType,
                       String entityType,
                       UUID entityId,
                       Map<String, Object> metadata) {

        Severity severity = event.defaultSeverity();
        String action = event.actionName();

        // ── کانال ۱: Log — پیش از هر چیز و هرگز قابل بازگشت ──
        logToSecurityStream(severity, action, tenantId, actorUserId,
                entityType, entityId, metadata);

        // ── کانال ۲: دیتابیس — تلاش بهترین‌کوشش ──
        try {
            requiresNewTransaction.executeWithoutResult(status -> {
                AuditLog entry = new AuditLog(action, actorType, severity);
                entry.setTenantId(tenantId);
                entry.setActorUserId(actorUserId);
                entry.setEntityType(entityType);
                entry.setEntityId(entityId);
                entry.setMetadata(toJson(metadata));
                entry.setTraceId(currentTraceId());

                HttpServletRequest request = currentRequest();
                if (request != null) {
                    entry.setIpAddress(request.getRemoteAddr());
                    entry.setUserAgent(truncate(
                            request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
                }

                repository.save(entry);
            });
        } catch (RuntimeException e) {
            // کانال ۱ از قبل نوشته شده، پس داده امنیتی از دست نرفته است.
            SECURITY.error("ثبت رویداد امنیتی {} در دیتابیس شکست خورد", action, e);
        }
    }

    private static void logToSecurityStream(Severity severity,
                                            String action,
                                            UUID tenantId,
                                            UUID actorUserId,
                                            String entityType,
                                            UUID entityId,
                                            Map<String, Object> metadata) {
        var builder = switch (severity) {
            case CRITICAL -> SECURITY.atError();
            case WARNING -> SECURITY.atWarn();
            case INFO -> SECURITY.atInfo();
        };
        builder.log("event={} tenantId={} actorId={} entity={}:{} metadata={}",
                action, tenantId, actorUserId, entityType, entityId, metadata);
    }

    /**
     * ثبت تلاش دسترسی بین‌مستأجری.
     *
     * <p>از {@code TenantGuard} صدا زده می‌شود، جایی که تصمیم گرفته شده
     * مالکیت رکورد با مستأجر جاری نمی‌خواند.
     *
     * @param entityType       نوع موجودیتی که به آن دسترسی خواسته شده
     * @param entityId         شناسه آن موجودیت
     * @param resourceTenantId مستأجری که واقعاً مالک رکورد است
     */
    public void idorAttempt(String entityType, UUID entityId, UUID resourceTenantId) {
        UUID currentTenantId = TenantContext.currentTenantId().orElse(null);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resourceTenantId", String.valueOf(resourceTenantId));
        metadata.put("currentTenantId", String.valueOf(currentTenantId));

        record(SecurityEvent.IDOR_ATTEMPT,
                currentTenantId,
                TenantContext.currentUserId().orElse(null),
                ActorType.USER,
                entityType,
                entityId,
                metadata);
    }

    /**
     * ثبت استفاده مجدد از توکن مصرف‌شده — نشانه سرقت توکن.
     *
     * @param actorUserId صاحب اصلی توکن
     * @param tenantId    مستأجر مربوطه
     * @param familyId    خانواده‌ای که باطل می‌شود
     */
    public void tokenReuseDetected(UUID actorUserId, UUID tenantId, UUID familyId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("familyId", String.valueOf(familyId));

        record(SecurityEvent.TOKEN_REUSE_DETECTED,
                tenantId, actorUserId, ActorType.USER, "RefreshToken", familyId, metadata);
    }

    /** ثبت ابطال یک خانواده توکن. */
    public void tokenFamilyRevoked(UUID actorUserId,
                                   UUID tenantId,
                                   UUID familyId,
                                   int revokedCount) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("familyId", String.valueOf(familyId));
        metadata.put("revokedCount", revokedCount);

        record(SecurityEvent.TOKEN_FAMILY_REVOKED,
                tenantId, actorUserId, ActorType.USER, "RefreshToken", familyId, metadata);
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            // اگر ساخت JSON شکست بخورد، خود رویداد را از دست نمی‌دهیم.
            SECURITY.error("ساخت JSON برای فراداده رویداد امنیتی شکست خورد", e);
            return null;
        }
    }

    private static String currentTraceId() {
        String traceId = MDC.get(MDC_TRACE_ID);
        return (traceId == null || traceId.isBlank())
                ? null
                : truncate(traceId, MAX_TRACE_ID_LENGTH);
    }

    /**
     * درخواست جاری، اگر در بستر HTTP باشیم.
     *
     * <p>در Jobهای زمان‌بندی‌شده هیچ درخواستی وجود ندارد، پس
     * {@code null} برمی‌گردد و فیلدهای IP و User-Agent خالی می‌مانند.
     *
     * <p><b>درباره IP:</b> عمداً {@code getRemoteAddr()} خوانده می‌شود و
     * نه هدر {@code X-Forwarded-For}. آن هدر را کلاینت می‌تواند جعل کند؛
     * اگر خودمان بخوانیمش، مهاجم می‌تواند در لاگ حسابرسی IP دلخواه
     * بنویسد. حل درستش در سطح زیرساخت انجام شده است:
     * {@code server.forward-headers-strategy: native} در پروفایل
     * production باعث می‌شود Spring خودش آدرس واقعی را از پروکسی معتمد
     * استخراج کند و در همان {@code getRemoteAddr()} بگذارد.
     */
    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return (value.length() <= maxLength) ? value : value.substring(0, maxLength);
    }
}
