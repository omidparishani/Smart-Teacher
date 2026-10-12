package ir.omid.smartteacher.common.event;

import java.util.UUID;

/**
 * رویداد دامنه — تکلیف منتشر شد (تریگر خودکارسازی فاز ۹).
 */
public record AssignmentPublishedEvent(
        UUID tenantId,
        UUID assignmentId,
        UUID classroomId,
        String title
) {
}
