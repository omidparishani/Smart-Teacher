package ir.omid.smartteacher.security.totp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

/**
 * سرویس TOTP برای 2FA سوپرادمین.
 */
@Service
public class TotpService {

    private final GoogleAuthenticator authenticator = new GoogleAuthenticator();
    private final TotpProperties properties;

    public TotpService(TotpProperties properties) {
        this.properties = properties;
    }

    public GeneratedSecret generateSecret(String accountName) {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        String secret = key.getKey();
        String otpAuthUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                properties.issuer(), accountName, key);
        return new GeneratedSecret(secret, otpAuthUrl);
    }

    public boolean verify(String secret, int code) {
        if (secret == null || secret.isBlank()) {
            return false;
        }
        return authenticator.authorize(secret, code);
    }

    public boolean verify(String secret, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        try {
            return verify(secret, Integer.parseInt(code.trim()));
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public boolean isEnforcementEnabled() {
        return properties.enabled();
    }

    public record GeneratedSecret(String secret, String otpAuthUrl) {
    }
}
