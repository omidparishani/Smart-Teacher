package ir.omid.smartteacher.ai.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.ai.internal.api.dto.CreatePromptRequest;
import ir.omid.smartteacher.ai.internal.api.dto.CreatePromptVersionRequest;
import ir.omid.smartteacher.ai.internal.api.dto.PromptResponse;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionCompareResponse;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionResponse;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionTestRequest;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionTestResponse;
import ir.omid.smartteacher.ai.internal.application.AdminPromptService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مدیریت پرامپت ادمین — {@code /api/v1/admin/prompts}.
 */
@RestController
@RequestMapping("/api/v1/admin/prompts")
@Tag(name = "پرامپت ادمین", description = "قالب، نسخه، انتشار، Rollback و Playground")
public class AdminPromptController {

    static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final AdminPromptService promptService;

    public AdminPromptController(AdminPromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فهرست قالب‌های پرامپت")
    public List<PromptResponse> list() {
        return promptService.list();
    }

    @PostMapping
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "ساخت قالب پرامپت")
    public PromptResponse create(@Valid @RequestBody CreatePromptRequest request) {
        return promptService.create(request);
    }

    @DeleteMapping("/{promptId}")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف قالب پرامپت")
    public void delete(@PathVariable UUID promptId) {
        promptService.delete(promptId);
    }

    @GetMapping("/{promptId}/versions")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فهرست نسخه‌های یک قالب")
    public List<PromptVersionResponse> listVersions(@PathVariable UUID promptId) {
        return promptService.listVersions(promptId);
    }

    @PostMapping("/{promptId}/versions")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "افزودن نسخه پیش‌نویس")
    public PromptVersionResponse addVersion(
            @PathVariable UUID promptId,
            @Valid @RequestBody CreatePromptVersionRequest request) {
        return promptService.addVersion(promptId, request);
    }

    @PostMapping("/versions/{versionId}/publish")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "انتشار نسخه")
    public PromptVersionResponse publish(@PathVariable UUID versionId) {
        return promptService.publish(versionId);
    }

    @PostMapping("/versions/{versionId}/rollback")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "بازگشت به نسخه آرشیوشده")
    public PromptVersionResponse rollback(@PathVariable UUID versionId) {
        return promptService.rollback(versionId);
    }

    @PostMapping("/versions/{versionId}/test")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "Playground — تست نسخه با Mock")
    public PromptVersionTestResponse test(
            @PathVariable UUID versionId,
            @RequestBody(required = false) PromptVersionTestRequest request) {
        return promptService.testVersion(
                versionId,
                request == null ? new PromptVersionTestRequest(null, null) : request);
    }

    @GetMapping("/versions/compare")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "مقایسه دو نسخه پرامپت")
    public PromptVersionCompareResponse compareVersions(
            @RequestParam UUID left,
            @RequestParam UUID right) {
        return promptService.compareVersions(left, right);
    }
}
