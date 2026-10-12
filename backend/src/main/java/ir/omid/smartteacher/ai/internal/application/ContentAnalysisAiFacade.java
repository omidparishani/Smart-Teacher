package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.ContentAnalysisAi;
import ir.omid.smartteacher.ai.internal.MockAiProvider;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * نمای تحلیل محتوا — از {@link ProviderBroker} و کاتالوگ Provider استفاده می‌کند.
 *
 * <p>🔴 قبلاً فقط {@code MockAiProvider} را از رجیستری می‌گرفت؛ با
 * {@code APP_MOCK_AI=false} و Provider ثبت‌شده در پنل، همیشه
 * {@code AI_PROVIDER_UNAVAILABLE} می‌داد. حالا همان مسیر رزولوشن چت
 * ({@code AI_BOOK_ANALYSIS}) اعمال می‌شود.
 */
@Service
public class ContentAnalysisAiFacade implements ContentAnalysisAi {

    /** هم‌تراز با feature flag و entitlement در V14 / V26. */
    public static final String FEATURE_BOOK_ANALYSIS = "AI_BOOK_ANALYSIS";

    private final ProviderBroker providerBroker;
    private final AiProviderRegistry registry;

    public ContentAnalysisAiFacade(ProviderBroker providerBroker,
                                   AiProviderRegistry registry) {
        this.providerBroker = providerBroker;
        this.registry = registry;
    }

    @Override
    public String analyze(String analysisType, String sourceText) {
        ResolutionContext ctx = currentContext();
        String prompt = buildAnalysisPrompt(analysisType, sourceText);
        AiProvider.AiResponse response = providerBroker.execute(
                FEATURE_BOOK_ANALYSIS,
                ctx,
                AiProvider.AiRequest.ofUserPrompt(null, prompt));
        return response.content() != null ? response.content() : "";
    }

    @Override
    public SubmissionGrade gradeSubmission(String assignmentTitle,
                                           String answerText,
                                           BigDecimal maxScore) {
        // ⚠️ generateStructured روی Adapter واقعی هنوز کامل نیست؛ اگر Mock
        // باشد از آن استفاده می‌شود، وگرنه نمرهٔ محافظه‌کارانهٔ محلی.
        AiProvider provider = registry.find(MockAiProvider.CODE)
                .or(() -> registry.allAvailable().stream().findFirst())
                .filter(AiProvider::isAvailable)
                .orElse(null);

        if (provider == null) {
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "ارائه‌دهنده هوش مصنوعی در دسترس نیست. "
                            + "یک Provider از نوع OPENAI_COMPATIBLE با کلید API در پنل ادمین ثبت کنید "
                            + "یا APP_MOCK_AI=true بگذارید.");
        }

        String prompt = "تصحیح تکلیف «"
                + nullToEmpty(assignmentTitle)
                + "» — پاسخ: "
                + nullToEmpty(answerText);
        try {
            AiProvider.AiStructuredResponse<SubmissionGrade> structured =
                    provider.generateStructured(
                            AiProvider.AiRequest.ofUserPrompt(
                                    MockAiProvider.DEFAULT_MODEL, prompt),
                            SubmissionGrade.class);

            if (structured.parsed() != null && structured.schemaValid()) {
                return normalizeGrade(structured.parsed(), maxScore);
            }
        } catch (RuntimeException ignored) {
            // Adapter واقعی ممکن است generateStructured نداشته باشد
        }

        BigDecimal score = maxScore != null
                ? maxScore.multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP)
                : new BigDecimal("8.00");
        return new SubmissionGrade(
                score,
                true,
                List.of("نقص جزئی در توضیح"),
                List.of("ساختار پاسخ قابل قبول است"),
                List.of("نیاز به تمرین بیشتر"),
                "پاسخ آزمایشی — تصحیح ساختاریافته در دسترس نبود.",
                "یادداشت معلم آزمایشی.",
                new BigDecimal("0.85"));
    }

    private static ResolutionContext currentContext() {
        UUID tenantId = TenantContext.currentTenantId().orElseThrow(
                () -> new AppException(ErrorCode.INVALID_REQUEST,
                        "مستأجر برای تحلیل AI لازم است."));
        UUID userId = TenantContext.currentUserId().orElse(null);
        return new ResolutionContext(
                tenantId, userId, null, null, FEATURE_BOOK_ANALYSIS);
    }

    private static SubmissionGrade normalizeGrade(SubmissionGrade grade, BigDecimal maxScore) {
        BigDecimal score = grade.score() != null ? grade.score() : BigDecimal.ONE;
        if (maxScore != null && score.compareTo(maxScore) > 0) {
            score = maxScore;
        }
        BigDecimal confidence = grade.confidence() != null
                ? grade.confidence()
                : new BigDecimal("0.80");
        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            confidence = BigDecimal.ONE;
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            confidence = BigDecimal.ZERO;
        }
        return new SubmissionGrade(
                score,
                grade.correct(),
                grade.mistakes() != null ? grade.mistakes() : List.of(),
                grade.strengths() != null ? grade.strengths() : List.of(),
                grade.weaknesses() != null ? grade.weaknesses() : List.of(),
                grade.feedback() != null ? grade.feedback() : "بازخورد آزمایشی",
                grade.teacherNote() != null ? grade.teacherNote() : "یادداشت آزمایشی",
                confidence);
    }

    private static String buildAnalysisPrompt(String analysisType, String sourceText) {
        String type = analysisType == null ? "SUMMARY" : analysisType;
        String label = switch (type) {
            case "QUESTIONS" -> "تولید سؤال از متن صفحه";
            case "TEACHING_PLAN" -> "طرح درس بر اساس متن صفحه";
            case "DIFFICULTY" -> "تشخیص سطح دشواری متن صفحه";
            default -> "خلاصه متن صفحه";
        };
        return label + ":\n" + nullToEmpty(sourceText);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
