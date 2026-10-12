package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.internal.domain.PromptVersionEntity;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * موتور جایگزینی متغیر پرامپت — {@code docs/prompt-system.md} §۳.
 *
 * <p>{@link Matcher#quoteReplacement} الزامی است تا {@code $1} در مقدار
 * متغیر خراب نشود. {@code UNTRUSTED_TEXT} داخل delimiter پیچیده می‌شود.
 */
@Component
public class PromptRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");
    private static final String UNTRUSTED_OPEN = "<student_answer>";
    private static final String UNTRUSTED_CLOSE = "</student_answer>";

    private final ObjectMapper objectMapper;

    public PromptRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RenderedPrompt render(PromptVersionEntity version, Map<String, Object> rawValues) {
        Map<String, VariableDef> defs = parseVariables(version.getVariables());
        Map<String, String> safe = new LinkedHashMap<>();

        for (Map.Entry<String, VariableDef> entry : defs.entrySet()) {
            String name = entry.getKey();
            VariableDef def = entry.getValue();
            Object raw = rawValues == null ? null : rawValues.get(name);
            if (raw == null || (raw instanceof String s && s.isBlank())) {
                if (def.required() && def.defaultValue() == null) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED,
                            "متغیر الزامی «" + name + "» مقدار ندارد.");
                }
                raw = def.defaultValue();
            }
            if (raw == null) {
                continue;
            }
            safe.put(name, sanitize(def, String.valueOf(raw)));
        }

        // متغیرهای ارسالی بدون تعریف — در صورت استفاده در قالب خطا می‌خورند
        if (rawValues != null) {
            for (Map.Entry<String, Object> e : rawValues.entrySet()) {
                if (!defs.containsKey(e.getKey()) && e.getValue() != null) {
                    safe.putIfAbsent(e.getKey(), String.valueOf(e.getValue()));
                }
            }
        }

        return new RenderedPrompt(
                substitute(version.getSystemPrompt(), safe, true),
                substitute(version.getUserPrompt(), safe, false));
    }

    private String substitute(String template, Map<String, String> values, boolean systemPrompt) {
        if (template == null) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            if (!values.containsKey(key)) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "متغیر ناشناخته «" + key + "» در پرامپت.");
            }
            String value = values.get(key);
            if (systemPrompt && value != null && value.contains(UNTRUSTED_OPEN)) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "UNTRUSTED_TEXT نباید در system_prompt بیاید.");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Map<String, VariableDef> parseVariables(String json) {
        Map<String, VariableDef> result = new HashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            root.properties().forEach(entry -> {
                JsonNode n = entry.getValue();
                String type = n.path("type").asString("STRING");
                boolean required = n.path("required").asBoolean(false);
                String def = n.has("default") && !n.get("default").isNull()
                        ? n.get("default").asString()
                        : null;
                int maxLength = n.path("maxLength").asInt(10_000);
                result.put(entry.getKey(), new VariableDef(type, required, def, maxLength));
            });
        } catch (Exception e) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "تعریف متغیرهای پرامپت نامعتبر است.");
        }
        return result;
    }

    private static String sanitize(VariableDef def, String raw) {
        String trimmed = raw;
        if (trimmed.length() > def.maxLength()) {
            trimmed = trimmed.substring(0, def.maxLength());
        }
        if ("UNTRUSTED_TEXT".equalsIgnoreCase(def.type())) {
            String cleaned = trimmed
                    .replace(UNTRUSTED_OPEN, "")
                    .replace(UNTRUSTED_CLOSE, "")
                    .replace('\u0000', ' ');
            return UNTRUSTED_OPEN + cleaned + UNTRUSTED_CLOSE;
        }
        return trimmed;
    }

    public record RenderedPrompt(String systemPrompt, String userPrompt) {
    }

    private record VariableDef(String type, boolean required, String defaultValue, int maxLength) {
    }
}
