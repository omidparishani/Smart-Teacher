package ir.omid.smartteacher.ai.internal.domain;

/**
 * نوع منطقی Provider — فقط برچسب پیکربندی، نه قرارداد HTTP واقعی.
 *
 * <p>Adapterهای واقعی پس از تأیید مستندات رسمی اضافه می‌شوند؛ تا آن
 * زمان فقط {@link #MOCK} در زمان اجرا استفاده می‌شود.
 */
public enum AiProviderType {
    MOCK,
    ANTHROPIC,
    OPENAI_COMPATIBLE,
    GOOGLE,
    CUSTOM
}
