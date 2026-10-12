package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.SystemSettingResponse;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateSystemSettingRequest;
import ir.omid.smartteacher.admin.internal.domain.SystemSetting;
import ir.omid.smartteacher.admin.internal.domain.SystemSettingRepository;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.tenant.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * مدیریت تنظیمات سراسری پلتفرم.
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
@Tag(name = "تنظیمات پلتفرم ادمین", description = "مشاهده و ویرایش تنظیمات سراسری سیستم")
public class AdminSettingsController {

    private final SystemSettingRepository settingRepository;

    public AdminSettingsController(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @GetMapping
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "فهرست تنظیمات سیستم")
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> list() {
        return settingRepository.findAllByOrderByCategoryAscKeyAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{key}")
    @PreAuthorize(AdminDashboardController.ADMIN_ONLY)
    @Operation(summary = "ویرایش یک تنظیم سیستم")
    @Transactional
    public SystemSettingResponse update(@PathVariable String key,
                                        @Valid @RequestBody UpdateSystemSettingRequest request) {
        SystemSetting setting = settingRepository.findByKey(key)
                .orElseThrow(() -> ResourceNotFoundException.of("تنظیم سیستم"));
        setting.setValue(request.value());
        setting.setUpdatedBy(TenantContext.currentUserId().orElse(null));
        return toResponse(settingRepository.save(setting));
    }

    private SystemSettingResponse toResponse(SystemSetting s) {
        // برای امنیت، اگر فیلد رمزنگاری‌شده بود مقدار خام افشا نشود
        String displayValue = s.isEncrypted() ? "********" : s.getValue();
        return new SystemSettingResponse(
                s.getId(),
                s.getKey(),
                displayValue,
                s.getValueType(),
                s.getCategory(),
                s.isEncrypted(),
                s.getDescription(),
                s.getUpdatedBy(),
                s.getUpdatedAt()
        );
    }
}
