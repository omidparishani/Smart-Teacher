package ir.omid.smartteacher.exam.internal.api.mapper;

import ir.omid.smartteacher.exam.internal.api.dto.ExamAnswerResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAttemptResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamQuestionResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamResponse;
import ir.omid.smartteacher.exam.internal.domain.Exam;
import ir.omid.smartteacher.exam.internal.domain.ExamAnswer;
import ir.omid.smartteacher.exam.internal.domain.ExamAttempt;
import ir.omid.smartteacher.exam.internal.domain.ExamQuestion;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** نگاشت موجودیت‌های آزمون به DTO. */
@Mapper(componentModel = "spring")
public interface ExamMapper {

    @Mapping(target = "questions", ignore = true)
    ExamResponse toResponse(Exam exam);

    ExamQuestionResponse toQuestionResponse(ExamQuestion question);

    @Mapping(target = "answers", ignore = true)
    ExamAttemptResponse toAttemptResponse(ExamAttempt attempt);

    ExamAnswerResponse toAnswerResponse(ExamAnswer answer);

    default ExamResponse toResponse(Exam exam, List<ExamQuestion> questions) {
        ExamResponse base = toResponse(exam);
        return new ExamResponse(
                base.id(), base.classroomId(), base.bookId(), base.title(), base.description(),
                base.durationMinutes(), base.totalScore(), base.startAt(), base.endAt(),
                base.status(), base.shuffleQuestions(), base.showResultsImmediately(),
                questions.stream().map(this::toQuestionResponse).toList(),
                base.createdAt());
    }

    default ExamAttemptResponse toAttemptResponse(ExamAttempt attempt, List<ExamAnswer> answers) {
        ExamAttemptResponse base = toAttemptResponse(attempt);
        return new ExamAttemptResponse(
                base.id(), base.examId(), base.studentId(), base.startedAt(), base.submittedAt(),
                base.score(), base.status(), base.attemptNumber(),
                answers.stream().map(this::toAnswerResponse).toList());
    }
}
