package ir.omid.smartteacher.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * پیکربندی هش رمز عبور — {@code docs/security.md} بخش ۵.
 *
 * <p>Argon2id با {@code m=19456 KiB}، {@code t=2}، {@code p=1}. این مقادیر
 * از سند امنیتی آمده‌اند و نباید سلیقه‌ای عوض شوند: تغییر پارامترها
 * هش‌های ذخیره‌شده را بی‌اعتبار می‌کند مگر مهاجرت صریح انجام شود.
 */
@Configuration
public class PasswordEncoderConfig {

    /** طول نمک — بایت. */
    private static final int SALT_LENGTH = 16;

    /** طول هش خروجی — بایت. */
    private static final int HASH_LENGTH = 32;

    /** موازی‌سازی — مطابق سند. */
    private static final int PARALLELISM = 1;

    /** حافظه به کیلوبایت — {@code m=19456}. */
    private static final int MEMORY_KIB = 19_456;

    /** تعداد تکرار — {@code t=2}. */
    private static final int ITERATIONS = 2;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS);
    }
}
