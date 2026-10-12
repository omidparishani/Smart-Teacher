package ir.omid.smartteacher.ai.internal.domain;

/**
 * سطوح رزولوشن Provider/Prompt — شش سطح طبق {@code docs/ai-providers.md}.
 */
public enum AiScopeType {
    USER,
    CLASS,
    TENANT,
    PLAN,
    FEATURE,
    GLOBAL
}
