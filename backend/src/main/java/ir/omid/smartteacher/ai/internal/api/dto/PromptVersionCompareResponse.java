package ir.omid.smartteacher.ai.internal.api.dto;

import java.util.List;
import java.util.UUID;

/** مقایسه دو نسخه پرامپت. */
public record PromptVersionCompareResponse(
        UUID leftVersionId,
        UUID rightVersionId,
        int leftVersionNumber,
        int rightVersionNumber,
        FieldDiff systemPrompt,
        FieldDiff userPrompt
) {

    public record FieldDiff(
            int leftLength,
            int rightLength,
            int lengthDelta,
            List<LineDiff> lineDiffs
    ) {
    }

    public record LineDiff(
            int lineNumber,
            String type,
            String leftLine,
            String rightLine
    ) {
    }
}
