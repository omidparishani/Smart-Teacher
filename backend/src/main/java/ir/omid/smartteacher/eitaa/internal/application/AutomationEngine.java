package ir.omid.smartteacher.eitaa.internal.application;

import ir.omid.smartteacher.common.event.AssignmentPublishedEvent;
import ir.omid.smartteacher.common.event.ExamCompletedEvent;
import ir.omid.smartteacher.common.event.ScoreBelowThresholdEvent;
import ir.omid.smartteacher.common.event.SubmissionEvaluatedEvent;
import ir.omid.smartteacher.common.event.SubscriptionExpiringEvent;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * موتور قواعد خودکارسازی — فقط رویدادهای داخلی؛ بدون webhook.
 */
@Component
public class AutomationEngine {

    private static final Logger log = LoggerFactory.getLogger(AutomationEngine.class);

    private final EitaaOutboundService outboundService;

    public AutomationEngine(EitaaOutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAssignmentPublished(AssignmentPublishedEvent event) {
        runInTenant(event.tenantId(), () -> {
            int n = outboundService.executeRulesForTrigger(
                    "ASSIGNMENT_PUBLISHED",
                    "تکلیف منتشر شد: " + event.title());
            log.debug("خودکارسازی ASSIGNMENT_PUBLISHED: {} قاعده", n);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onExamCompleted(ExamCompletedEvent event) {
        runInTenant(event.tenantId(), () -> {
            int n = outboundService.executeRulesForTrigger(
                    "EXAM_COMPLETED",
                    "آزمون تکمیل شد — نمره: " + event.score());
            log.debug("خودکارسازی EXAM_COMPLETED: {} قاعده", n);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSubmissionEvaluated(SubmissionEvaluatedEvent event) {
        runInTenant(event.tenantId(), () -> {
            int n = outboundService.executeRulesForTrigger(
                    "SUBMISSION_EVALUATED",
                    "تصحیح تکلیف «" + event.assignmentTitle() + "» — نمره پیشنهادی: "
                            + event.suggestedScore());
            log.debug("خودکارسازی SUBMISSION_EVALUATED: {} قاعده", n);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onScoreBelowThreshold(ScoreBelowThresholdEvent event) {
        runInTenant(event.tenantId(), () -> {
            int n = outboundService.executeRulesForTrigger(
                    "SCORE_BELOW_THRESHOLD",
                    "نمره زیر آستانه: " + event.percent() + "%",
                    AutomationTriggerContext.withScorePercent(event.percent()));
            log.debug("خودکارسازی SCORE_BELOW_THRESHOLD: {} قاعده", n);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSubscriptionExpiring(SubscriptionExpiringEvent event) {
        runInTenant(event.tenantId(), () -> {
            int n = outboundService.executeRulesForTrigger(
                    "SUBSCRIPTION_EXPIRING",
                    "اشتراک تا " + event.daysRemaining() + " روز دیگر منقضی می‌شود");
            log.debug("خودکارسازی SUBSCRIPTION_EXPIRING: {} قاعده", n);
        });
    }

    private static void runInTenant(UUID tenantId, Runnable action) {
        TenantPrincipal principal = TenantPrincipal.ofTenantUser(
                null, tenantId, null, Set.of(), MemberRole.TEACHER, "fa");
        TenantContext.runAs(principal, action);
    }
}
