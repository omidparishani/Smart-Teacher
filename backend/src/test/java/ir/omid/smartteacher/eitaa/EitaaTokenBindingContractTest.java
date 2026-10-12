package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * فاز ۹ / گام A3 — قفل‌کردن نگاشت نام متغیرهای توکن ایتا.
 *
 * <p>🔴 این آزمون از یک نقص <b>واقعی و بی‌صدا</b> زاده شد که در بازبینی
 * پیکربندی Production پیدا شد:
 *
 * <p>کلید {@code app.eitaa.bot-token} در {@code application.yml} تعریف
 * نشده و بلوک {@code app.eitaa} فقط در {@code application-dev.yml} بود.
 * در نتیجه در پروفایل {@code prod} هیچ نگاشتی برای نام کوتاه وجود نداشت
 * و {@code EITAA_BOT_TOKEN} — همان نامی که سه سند آموزش می‌دهند —
 * <b>بی‌صدا نادیده گرفته می‌شد</b>.
 *
 * <p>چرا بی‌صدا بود و چرا خطرناک: {@code EitaaYarBotAdapter} در نبود توکن
 * فقط {@code log.warn} می‌دهد و {@code LocalHmacVerifier} در زمان
 * <i>درخواست</i> استثنا می‌اندازد. پس استقرار سالم بالا می‌آمد،
 * {@code /actuator/health} سبز بود (این مسیر به ایتا وابسته نیست)، و
 * کاربر تازه موقع اولین ارسال خطا می‌دید.
 *
 * <p>این آزمون همان چیزی را می‌سنجد که واقعاً شکسته بود: <b>محتوای فایل
 * پیکربندی</b>. عمداً بدون Spring context اجرا می‌شود — چون بارگذاری
 * پروفایل {@code prod} به {@code DB_URL} و {@code JWT_SECRET} و
 * {@code ENCRYPTION_MASTER_KEY} نیاز دارد و آزمون را شکننده می‌کرد.
 */
@DisplayName("قرارداد نگاشت توکن‌های ایتا در پروفایل‌های Spring")
class EitaaTokenBindingContractTest {

    private static final String BOT_TOKEN_KEY = "bot-token";
    private static final String APP_TOKEN_KEY = "app-token";

    /** نام canonical — با relaxed binding در هر پروفایلی کار می‌کند، بدون هیچ کلید yml. */
    private static final String CANONICAL_BOT_VAR = "APP_EITAA_BOT_TOKEN";
    private static final String CANONICAL_APP_VAR = "APP_EITAA_APP_TOKEN";

    @Test
    @DisplayName("پروفایل prod هر دو توکن را نگاشت می‌کند — وگرنه نام مستندشده بی‌اثر است")
    void prodProfile_mapsBothTokens() {
        Map<String, Object> prod = load("application-prod.yml");

        assertThat(placeholder(prod, BOT_TOKEN_KEY))
                .as("prod باید app.eitaa.bot-token را تعریف کند؛ نبودش یعنی "
                        + "EITAA_BOT_TOKEN در Production بی‌صدا نادیده گرفته می‌شود")
                .isNotBlank();
        assertThat(placeholder(prod, APP_TOKEN_KEY))
                .as("prod باید app.eitaa.app-token را تعریف کند")
                .isNotBlank();
    }

    @Test
    @DisplayName("نگاشت prod نام canonical را می‌خواند، نه فقط نام کوتاه")
    void prodProfile_readsCanonicalNames() {
        Map<String, Object> prod = load("application-prod.yml");

        // اگر کسی نام کوتاه را تنها بگذارد، نگاشت به نامی می‌بندد که
        // خودش نیاز به نگاشت دارد — یعنی حلقه. نام canonical باید باشد.
        assertThat(placeholder(prod, BOT_TOKEN_KEY)).contains(CANONICAL_BOT_VAR);
        assertThat(placeholder(prod, APP_TOKEN_KEY)).contains(CANONICAL_APP_VAR);
    }

    @Test
    @DisplayName("dev و prod روی کلیدهای app.eitaa توافق دارند (هیچ پروفایلی عقب نمی‌ماند)")
    void devAndProdAgree() {
        Map<String, Object> dev = load("application-dev.yml");
        Map<String, Object> prod = load("application-prod.yml");

        for (String key : new String[] {BOT_TOKEN_KEY, APP_TOKEN_KEY}) {
            assertThat(placeholder(dev, key))
                    .as("dev باید %s را نگاشت کند", key)
                    .isNotBlank();
            assertThat(placeholder(prod, key))
                    .as("prod باید %s را نگاشت کند — ناهم‌خوانی dev/prod همان "
                            + "نقصی بود که سه سند را گمراه می‌کرد", key)
                    .isNotBlank();
        }
    }

    // ─── ابزار ───

    private static String placeholder(Map<String, Object> root, String key) {
        Map<String, Object> eitaa = child(child(root, "app"), "eitaa");
        Object value = (eitaa == null) ? null : eitaa.get(key);
        return (value == null) ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> node, String key) {
        if (node == null) {
            return null;
        }
        Object value = node.get(key);
        return (value instanceof Map) ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(String resource) {
        try (InputStream in = EitaaTokenBindingContractTest.class
                .getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("منبع «%s» باید روی classpath باشد", resource).isNotNull();
            return (Map<String, Object>) new Yaml().load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("خواندن " + resource + " شکست خورد", e);
        }
    }
}
