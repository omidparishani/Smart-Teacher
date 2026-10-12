package ir.omid.smartteacher.student.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * آزمون رقم کنترلی کد ملی — مستقل از Spring و دیتابیس.
 *
 * <p>این تست قفل می‌کند که الگوریتم عوض نشود و کدهای همه‌رقم‌یکسان
 * رد بمانند. بدون آن، یک refactor بی‌صدا می‌تواند همه کدها را بپذیرد.
 */
class NationalCodeTest {

    @Test
    @DisplayName("کد ملی معتبر با رقم کنترلی درست پذیرفته می‌شود")
    void validCodeIsAccepted() {
        // 0123456789: sum=156, 156%11=2, check=9
        assertThat(NationalCode.isValid("0123456789")).isTrue();
    }

    @Test
    @DisplayName("رقم کنترلی اشتباه رد می‌شود")
    void wrongCheckDigitIsRejected() {
        assertThat(NationalCode.isValid("0123456780")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "01234567890", "abcdefghij", "012345678 ", " 0123456789"})
    @DisplayName("ورودی‌های ناقص، بلند، حروفی یا با فاصله رد می‌شوند")
    void malformedCodesAreRejected(String code) {
        assertThat(NationalCode.isValid(code)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000000000", "1111111111", "9999999999"})
    @DisplayName("کدهای همه‌رقم‌یکسان رد می‌شوند حتی اگر رقم کنترلی جور باشد")
    void allIdenticalDigitsAreRejected(String code) {
        assertThat(NationalCode.isValid(code)).isFalse();
    }
}
