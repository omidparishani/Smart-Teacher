package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** زمان‌بندی پیام — ذخیره + ارسال با پارامتر date. */
public record ScheduleMessageRequest(
        @NotNull UUID groupId,
        @NotBlank String text,
        @NotNull Instant scheduledAt
) {}
