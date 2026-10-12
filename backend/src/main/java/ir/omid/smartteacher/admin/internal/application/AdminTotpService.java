package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.security.totp.TotpService;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.user.User;
import ir.omid.smartteacher.user.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * فعال‌سازی 2FA برای سوپرادمین.
 */
@Service
public class AdminTotpService {

    private final UserRepository userRepository;
    private final TotpService totpService;

    public AdminTotpService(UserRepository userRepository, TotpService totpService) {
        this.userRepository = userRepository;
        this.totpService = totpService;
    }

    /**
     * شروع فعال‌سازی — تولید secret و otpauth URL (هنوز enabled=false).
     */
    @Transactional
    public TotpSetupResponse beginSetup() {
        User user = requireCurrentUser();
        TotpService.GeneratedSecret generated = totpService.generateSecret(
                user.getUsername() != null ? user.getUsername() : user.getId().toString());
        user.setTotpSecret(generated.secret());
        user.setTotpEnabled(false);
        return new TotpSetupResponse(generated.secret(), generated.otpAuthUrl());
    }

    /**
     * تأیید کد و روشن کردن totp_enabled.
     */
    @Transactional
    public void confirmSetup(String code) {
        User user = requireCurrentUser();
        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "ابتدا راه‌اندازی 2FA را شروع کنید.");
        }
        if (!totpService.verify(user.getTotpSecret(), code)) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "کد 2FA نامعتبر است.");
        }
        user.setTotpEnabled(true);
    }

    private User requireCurrentUser() {
        UUID userId = TenantContext.requirePrincipal().userId();
        return userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::generic);
    }

    public record TotpSetupResponse(String secret, String otpAuthUrl) {
        @Override
        public String toString() {
            return "TotpSetupResponse{secret=[REDACTED]}";
        }
    }
}
