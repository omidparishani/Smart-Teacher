package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.EitaaInitDataInvalidException;
import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * پذیرش {@code initData} معتبر.
 *
 * <p>این کلاس طرف «باید بپذیرد» است؛ رد کردن‌ها کلاس‌های جدا دارند.
 * تفکیکشان عمدی است: اگر همه در یک کلاس بودند، یک تغییر در آستانه
 * تازگی می‌توانست هم‌زمان چند آزمون را با هم ببرد و معلوم نمی‌شد کدام
 * رفتار عوض شده.
 */
class EitaaInitDataValidationTest {

    /** کاربر واقعی ایتا می‌تواند شناسه بزرگ‌تر از {@code int} داشته باشد. */
    private static final long USER_ID = 4_503_599_627_370_495L;

    private static EitaaInitDataValidator validator(EitaaProperties properties) {
        return new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());
    }

    private static EitaaInitDataValidator validator() {
        return validator(EitaaTestData.botProperties());
    }

    // ─────────────────────────── مسیر خوش‌بینانه ───────────────────────────

    @Nested
    @DisplayName("initData معتبر")
    class Acceptance {

        @Test
        @DisplayName("پذیرفته می‌شود و کاربر را برمی‌گرداند")
        void acceptsValidInitData() {
            EitaaInitData result = validator().validate(EitaaTestData.validInitData(USER_ID));

            assertThat(result.user().id()).isEqualTo(USER_ID);
            assertThat(result.user().firstName()).isEqualTo("علی");
            assertThat(result.user().lastName()).isEqualTo("احمدی");
            assertThat(result.user().languageCode()).isEqualTo("fa");
            assertThat(result.user().hasWriteAccess()).isTrue();
        }

        @Test
        @DisplayName("زمان صدور گزارش می‌شود")
        void reportsAuthDate() {
            Instant issuedAt = Instant.now().minusSeconds(30);

            // ⚠️ auth_date یونیکس‌ثانیه است، پس دقتی کوچک‌تر از ثانیه
            // وجود ندارد: هر نانوثانیه‌ای که در issuedAt بگذاریم، در
            // رفت‌وبرگشت گم می‌شود. انتظار هم باید همان لحظه‌ی
            // ثانیه‌تراشیده باشد، وگرنه آزمون چیزی را می‌سنجد که
            // قرارداد پروتکل هرگز وعده نداده.
            Instant expected = Instant.ofEpochSecond(issuedAt.getEpochSecond());

            String initData = EitaaTestData.signed(
                    EitaaTestData.baseParams(USER_ID, issuedAt));

            assertThat(validator().validate(initData).authDate())
                    .as("auth_date باید ثانیه‌به‌ثانیه برگردد")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("پارامترهای اختیاری خوانده می‌شوند")
        void readsOptionalParams() {
            Map<String, String> params = EitaaTestData.baseParams(USER_ID);
            params.put("query_id", "AAF-query-id");
            params.put("start_param", "asg_123");
            params.put("chat_type", "group");
            params.put("device_id", "device-abc");

            EitaaInitData result = validator().validate(EitaaTestData.signed(params));

            assertThat(result.queryId()).isEqualTo("AAF-query-id");
            assertThat(result.startParam()).isEqualTo("asg_123");
            assertThat(result.chatType()).isEqualTo("group");
            assertThat(result.deviceId()).isEqualTo("device-abc");
            assertThat(result.hasStartParam()).isTrue();
        }

        @Test
        @DisplayName("نبودِ پارامترهای اختیاری خطا نیست")
        void optionalParamsMayBeAbsent() {
            Map<String, String> params = EitaaTestData.baseParams(USER_ID);
            params.remove("chat_type");

            EitaaInitData result = validator().validate(EitaaTestData.signed(params));

            assertThat(result.chatType()).isNull();
            assertThat(result.startParam()).isNull();
            assertThat(result.hasStartParam()).isFalse();
            assertThat(result.deviceId()).isNull();
        }

        @Test
        @DisplayName("اثر انگشت یکتا برگردانده می‌شود")
        void reportsFingerprint() {
            EitaaInitData result = validator().validate(EitaaTestData.validInitData(USER_ID));

            assertThat(result.fingerprint())
                    .as("اثر انگشت باید هش SHA-256 باشد")
                    .hasSize(64)
                    .matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("کاربر بدون نام و نام کاربری هم پذیرفته می‌شود")
        void acceptsUserWithoutName() {
            // ⚠️ سند رسمی فقط id را تضمین می‌کند. اگر نبودِ نام، ورود را
            // بشکند، کاربرانی که نامشان را در ایتا خالی گذاشته‌اند
            // هرگز نمی‌توانند وارد شوند — و دلیلش هیچ‌جا پیدا نمی‌شود.
            Map<String, String> params = EitaaTestData.baseParams(USER_ID);
            params.put("user", EitaaTestData.encode("{\"id\":" + USER_ID + "}"));

            EitaaInitData result = validator().validate(EitaaTestData.signed(params));

            assertThat(result.user().id()).isEqualTo(USER_ID);
            assertThat(result.user().firstName()).isNull();
            assertThat(result.user().displayName()).isEqualTo(String.valueOf(USER_ID));
        }
    }

    // ─────────────────────────── ورودی ناقص ───────────────────────────

    @Nested
    @DisplayName("initData ناقص")
    class MissingInput {

        @Test
        @DisplayName("initData خالی رد می‌شود")
        void rejectsEmpty() {
            EitaaInitDataValidator validator = validator();

            assertThatThrownBy(() -> validator.validate(""))
                    .isInstanceOf(EitaaInitDataInvalidException.class);
            assertThatThrownBy(() -> validator.validate("   "))
                    .isInstanceOf(EitaaInitDataInvalidException.class);
            assertThatThrownBy(() -> validator.validate(null))
                    .isInstanceOf(EitaaInitDataInvalidException.class);
        }

        @Test
        @DisplayName("نبودِ hash رد می‌شود")
        void rejectsMissingHash() {
            Map<String, String> params = EitaaTestData.baseParams(USER_ID);
            String withoutHash = String.join("&",
                    params.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .toList());

            assertThatThrownBy(() -> validator().validate(withoutHash))
                    .isInstanceOf(EitaaInitDataInvalidException.class);
        }

        @Test
        @DisplayName("نبودِ user رد می‌شود")
        void rejectsMissingUser() {
            Map<String, String> params = EitaaTestData.baseParams(USER_ID);
            params.remove("user");

            assertThatThrownBy(() -> validator().validate(EitaaTestData.signed(params)))
                    .isInstanceOf(EitaaInitDataInvalidException.class);
        }

        @Test
        @DisplayName("نبودِ auth_date رد می‌شود")
        void rejectsMissingAuthDate() {
            Map<String, String> params = EitaaTestData.baseParams(USER_ID);
            params.remove("auth_date");

            assertThatThrownBy(() -> validator().validate(EitaaTestData.signed(params)))
                    .isInstanceOf(EitaaInitDataInvalidException.class);
        }

        @Test
        @DisplayName("auth_date غیرعددی رد می‌شود")
        void rejectsNonNumericAuthDate() {
            Map<String, String> params = EitaaTestData.baseParams(USER_ID);
            params.put("auth_date", "دیروز");

            assertThatThrownBy(() -> validator().validate(EitaaTestData.signed(params)))
                    .isInstanceOf(EitaaInitDataInvalidException.class);
        }
    }
}
