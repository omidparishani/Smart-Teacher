package ir.omid.smartteacher.eitaa.internal.application;

import java.math.BigDecimal;

/** زمینه اختیاری برای فیلتر قواعد خودکارسازی. */
public record AutomationTriggerContext(BigDecimal scorePercent) {

    public static final AutomationTriggerContext NONE = new AutomationTriggerContext(null);

    public static AutomationTriggerContext withScorePercent(BigDecimal percent) {
        return new AutomationTriggerContext(percent);
    }
}
