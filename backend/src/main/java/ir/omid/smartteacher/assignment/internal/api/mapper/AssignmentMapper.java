package ir.omid.smartteacher.assignment.internal.api.mapper;

import ir.omid.smartteacher.assignment.internal.api.dto.AssignmentResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.SubmissionResponse;
import ir.omid.smartteacher.assignment.internal.domain.Assignment;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * نگاشت موجودیت‌های تکلیف به DTO.
 */
@Mapper(componentModel = "spring")
public interface AssignmentMapper {

    @Mapping(target = "classroomName", ignore = true)
    @Mapping(target = "submissionCount", ignore = true)
    @Mapping(target = "eitaaMessageStatus", source = "eitaaSendStatus")
    AssignmentResponse toResponse(Assignment entity);

    default AssignmentResponse toResponse(Assignment entity,
                                          String classroomName,
                                          long submissionCount) {
        AssignmentResponse base = toResponse(entity);
        return new AssignmentResponse(
                base.id(),
                base.classroomId(),
                classroomName,
                base.bookId(),
                base.pageFrom(),
                base.pageTo(),
                base.title(),
                base.description(),
                base.type(),
                base.maxScore(),
                base.dueAt(),
                base.status(),
                base.aiEvaluationEnabled(),
                submissionCount,
                base.eitaaChatId(),
                base.eitaaMessageStatus(),
                base.createdAt(),
                base.updatedAt());
    }

    @Mapping(target = "studentName", ignore = true)
    SubmissionResponse toSubmissionResponse(AssignmentSubmission entity);

    default SubmissionResponse toSubmissionResponse(AssignmentSubmission entity,
                                                    String studentName) {
        SubmissionResponse base = toSubmissionResponse(entity);
        return new SubmissionResponse(
                base.id(),
                base.assignmentId(),
                base.studentId(),
                studentName,
                base.answerText(),
                base.status(),
                base.submittedAt(),
                base.submittedByUserId(),
                base.finalScore(),
                base.teacherOverrodeAi());
    }
}
