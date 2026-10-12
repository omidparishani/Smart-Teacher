package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** اتصال گروه به کلاس. */
public record ConnectClassRequest(@NotNull UUID classroomId) {}
