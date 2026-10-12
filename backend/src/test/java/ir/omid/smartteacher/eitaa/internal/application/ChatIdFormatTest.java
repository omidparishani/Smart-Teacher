package ir.omid.smartteacher.eitaa.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * بررسی قالب {@code chat_id} — مستقل از Spring و دیتابیس.
 *
 * <p>🔴 این آزمون یک رگرسیون واقعی را قفل می‌کند. نسخهٔ پیشین
 * {@code EitaaOutboundService} الگوی {@code -?@?[A-Za-z0-9_]{3,100}} را
 * داشت که {@code "@group-1"} را نامعتبر می‌خواند — دقیقاً همان شکلی که
 * خودِ آزمون‌های این پروژه می‌سازند ({@code "@group-" + unique()}). یعنی
 * اگر کسی آن اعتبارسنجی را به مسیر ساخت/ویرایش وصل می‌کرد، پنج آزمون
 * موجود می‌شکست. ریشهٔ اشکال مفهومی بود: سند رسمی ایتا مجموعه‌نویسهٔ
 * مجاز {@code chat_id} را مشخص نمی‌کند، پس هر الگوی دقیقی «حدس» است و
 * طبق قاعدهٔ پروژه («هیچ API‌ای حدس زده نمی‌شود») مجاز نیست.
 *
 * <p>در عوض، این آزمون مرز را قفل می‌کند: فقط ورودی‌ای رد می‌شود که
 * <b>قطعاً</b> غلط است.
 */
class ChatIdFormatTest {

    @Test
    @DisplayName("شناسه‌های واقعی ایتا — با @، بدون @، عددی، دارای خط تیره — پذیرفته می‌شوند")
    void realIds_areAccepted() {
        // شکلی که آزمون‌های موجود پروژه می‌سازند — رگرسیون اصلی.
        assertThat(EitaaOutboundService.isValidChatId("@group-1")).isTrue();
        assertThat(EitaaOutboundService.isValidChatId("@channel-7")).isTrue();
        // شناسهٔ عددی خام — همان چیزی که کاربر واقعی از ایتا کپی می‌کند.
        assertThat(EitaaOutboundService.isValidChatId("11268746")).isTrue();
        // شناسهٔ عددی گروه با پیشوند منفی.
        assertThat(EitaaOutboundService.isValidChatId("-1001234567890")).isTrue();
        // نام کاربری بدون @.
        assertThat(EitaaOutboundService.isValidChatId("my_channel")).isTrue();
        // فاصلهٔ ابتدا/انتها نادیده گرفته می‌شود (trim).
        assertThat(EitaaOutboundService.isValidChatId(" 11268746 ")).isTrue();
    }

    @Test
    @DisplayName("null و رشتهٔ خالی/فقط‌فاصله رد می‌شوند")
    void blank_isRejected() {
        assertThat(EitaaOutboundService.isValidChatId(null)).isFalse();
        assertThat(EitaaOutboundService.isValidChatId("")).isFalse();
        assertThat(EitaaOutboundService.isValidChatId("   ")).isFalse();
    }

    @Test
    @DisplayName("فاصلهٔ داخلی رد می‌شود — نشانهٔ اینکه کاربر عنوان را جای شناسه داده")
    void internalWhitespace_isRejected() {
        assertThat(EitaaOutboundService.isValidChatId("گروه ریاضی")).isFalse();
        assertThat(EitaaOutboundService.isValidChatId("two words")).isFalse();
        // تب هم فاصله است، نه نویسهٔ مجاز.
        assertThat(EitaaOutboundService.isValidChatId("my\tchannel")).isFalse();
    }

    @Test
    @DisplayName("لینک رد می‌شود — کاربر باید شناسه بدهد، نه URL")
    void url_isRejected() {
        assertThat(EitaaOutboundService.isValidChatId("https://eitaa.com/foo")).isFalse();
        assertThat(EitaaOutboundService.isValidChatId("http://eitaa.com/foo")).isFalse();
        // حساس به بزرگ/کوچک نباشد — کاربر ممکن است با حروف بزرگ بنویسد.
        assertThat(EitaaOutboundService.isValidChatId("HTTPS://eitaa.com/foo")).isFalse();
    }

    @Test
    @DisplayName("طول بیش از ۱۰۰ نویسه رد می‌شود — سقف ستون پایگاه داده")
    void tooLong_isRejected() {
        // ستون `eitaa_chat_id` در پایگاه داده `length = 100` است.
        assertThat(EitaaOutboundService.isValidChatId("x".repeat(100))).isTrue();
        assertThat(EitaaOutboundService.isValidChatId("x".repeat(101))).isFalse();
    }
}
