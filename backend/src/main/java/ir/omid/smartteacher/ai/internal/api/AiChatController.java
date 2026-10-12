package ir.omid.smartteacher.ai.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.internal.api.dto.AiChatRequest;
import ir.omid.smartteacher.ai.internal.api.dto.AiChatResponse;
import ir.omid.smartteacher.ai.internal.application.AiChatService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * دستیار هوشمند معلم — {@code POST /api/v1/ai/chat}.
 *
 * <p>فقط نقش‌های کارمند مستأجر (معلم/مالک/مدیر مدرسه). دانش‌آموز ۴۰۳ می‌گیرد.
 *
 * <p><b>⚠️ رشته دسترسی اینجا تکرار شده</b> و عمدی است: کپی از ثابت
 * داخلی ماژول {@code classroom} مرز Modulith را می‌شکست. اگر روزی
 * سطوح دسترسی یکی شدند، جایش یک ثابت مشترک در ماژول باز است — نه ارجاع
 * به {@code classroom.internal}.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "دستیار هوشمند", description = "گفت‌وگو با دستیار — فاز ۵ گام ۱ فقط Mock")
@ConditionalOnBean(AiProvider.class)
public class AiChatController {

    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER')";

    private final AiChatService chatService;

    public AiChatController(AiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "گفت‌وگو با دستیار",
            description = "در این گام فقط MockAiProvider پاسخ می‌دهد و مصرف ثبت می‌شود.")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return chatService.chat(request);
    }
}
