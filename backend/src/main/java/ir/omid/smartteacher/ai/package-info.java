/**
 * ماژول هوش مصنوعی — Provider، مدل، مصرف و دستیار معلم.
 *
 * <p>این ماژول {@code CLOSED} است: فقط بسته‌ی ریشه برای بیرون دیده می‌شود
 * ({@link ir.omid.smartteacher.ai.AiProvider} و مدل‌های درخواست/پاسخ).
 * موجودیت‌ها، مخزن‌ها و کنترلرها در {@code ai.internal} پنهان‌اند تا
 * ماژول‌های {@code classroom}/{@code student} به جزئیات Provider یا
 * جدول مصرف وابسته نشوند.
 *
 * <p><b>⚠️ قید فاز ۵ گام ۱:</b> فقط {@code MockAiProvider} پیاده می‌شود.
 * هیچ Adapter واقعی و هیچ نام فیلد JSON فرضی برای Anthropic/OpenAI
 * اینجا نیست ({@code docs/verification-log.md} بخش ۴).
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI")
package ir.omid.smartteacher.ai;
