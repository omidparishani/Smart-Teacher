package ir.omid.smartteacher.exam.internal.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartExamRequest(@NotNull UUID studentId) {}
