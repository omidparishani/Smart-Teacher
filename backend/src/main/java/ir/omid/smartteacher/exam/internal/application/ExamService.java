package ir.omid.smartteacher.exam.internal.application;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.event.ExamCompletedEvent;
import ir.omid.smartteacher.common.event.ScoreBelowThresholdEvent;
import ir.omid.smartteacher.eitaa.EitaaAdapter;
import ir.omid.smartteacher.eitaa.EitaaSendResult;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamQuestionRequest;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAnswerResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAttemptReportResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAttemptResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamBlueprintResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamQuestionResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamResultsResponse;
import ir.omid.smartteacher.exam.internal.api.dto.GenerateExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.GradeAttemptRequest;
import ir.omid.smartteacher.exam.internal.api.dto.PublishExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.StartExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.SubmitAnswerRequest;
import ir.omid.smartteacher.exam.internal.api.dto.UpdateExamRequest;
import ir.omid.smartteacher.exam.internal.api.mapper.ExamMapper;
import ir.omid.smartteacher.exam.internal.domain.AttemptStatus;
import ir.omid.smartteacher.exam.internal.domain.Exam;
import ir.omid.smartteacher.exam.internal.domain.ExamAnswer;
import ir.omid.smartteacher.exam.internal.domain.ExamAnswerRepository;
import ir.omid.smartteacher.exam.internal.domain.ExamAttempt;
import ir.omid.smartteacher.exam.internal.domain.ExamAttemptRepository;
import ir.omid.smartteacher.exam.internal.domain.ExamBlueprint;
import ir.omid.smartteacher.exam.internal.domain.ExamBlueprintRepository;
import ir.omid.smartteacher.exam.internal.domain.ExamQuestion;
import ir.omid.smartteacher.exam.internal.domain.ExamQuestionRepository;
import ir.omid.smartteacher.exam.internal.domain.ExamRepository;
import ir.omid.smartteacher.exam.internal.domain.ExamStatus;
import ir.omid.smartteacher.exam.internal.domain.QuestionType;
import ir.omid.smartteacher.security.TenantGuard;
import ir.omid.smartteacher.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * منطق کسب‌وکار آزمون — فاز ۸ (تولید AI، تصحیح، نتایج، مهلت).
 */
@Service
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private static final Set<String> SORTABLE =
            Set.of("title", "status", "createdAt", "startAt");
    private static final BigDecimal SCORE_THRESHOLD_PERCENT = new BigDecimal("50");

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamAttemptRepository attemptRepository;
    private final ExamAnswerRepository answerRepository;
    private final ExamBlueprintRepository blueprintRepository;
    private final ExamMapper mapper;
    private final TenantGuard tenantGuard;
    private final ObjectProvider<AiProvider> aiProvider;
    private final ApplicationEventPublisher events;
    private final EitaaAdapter eitaaAdapter;

    public ExamService(ExamRepository examRepository,
                       ExamQuestionRepository questionRepository,
                       ExamAttemptRepository attemptRepository,
                       ExamAnswerRepository answerRepository,
                       ExamBlueprintRepository blueprintRepository,
                       ExamMapper mapper,
                       TenantGuard tenantGuard,
                       ObjectProvider<AiProvider> aiProvider,
                       ApplicationEventPublisher events,
                       EitaaAdapter eitaaAdapter) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.blueprintRepository = blueprintRepository;
        this.mapper = mapper;
        this.tenantGuard = tenantGuard;
        this.aiProvider = aiProvider;
        this.events = events;
        this.eitaaAdapter = eitaaAdapter;
    }

    // ─────────────────────────── معلم ───────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ExamResponse> list(int page, int size, String sort,
                                           UUID classroomId, ExamStatus status) {
        Pageable pageable = PageableFactory.of(page, size, sort, SORTABLE, "createdAt,desc");
        Page<Exam> result = examRepository.findForListing(classroomId, status, pageable);
        return PageResponse.from(result, exam -> mapper.toResponse(exam, List.of()));
    }

    @Transactional(readOnly = true)
    public ExamResponse get(UUID id) {
        Exam exam = requireExam(id);
        return mapper.toResponse(exam, questionRepository.findByExamIdOrderByOrderIndexAsc(id));
    }

    @Transactional
    public ExamResponse create(CreateExamRequest request) {
        Exam exam = new Exam(request.classroomId(), request.title().trim());
        exam.setDescription(trimToNull(request.description()));
        exam.setDurationMinutes(request.durationMinutes());
        exam.setTotalScore(request.totalScore());
        exam.setStartAt(request.startAt());
        exam.setEndAt(request.endAt());
        exam.setBookId(request.bookId());
        if (request.shuffleQuestions() != null) {
            exam.setShuffleQuestions(request.shuffleQuestions());
        }
        if (request.showResultsImmediately() != null) {
            exam.setShowResultsImmediately(request.showResultsImmediately());
        }
        exam.setStatus(ExamStatus.DRAFT);
        return mapper.toResponse(examRepository.save(exam), List.of());
    }

    /**
     * تولید آزمون از کتاب/بازه صفحه با Mock AI — چند MCQ + یک تشریحی.
     */
    @Transactional
    public ExamResponse generate(GenerateExamRequest request) {
        if (request.pageTo() < request.pageFrom()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "بازه صفحه نامعتبر است (pageTo باید ≥ pageFrom باشد).");
        }

        int mcqCount = request.mcqCount() != null ? request.mcqCount() : 3;
        int trueFalseCount = request.trueFalseCount() != null ? request.trueFalseCount() : 0;

        // فراخوانی Mock AI تا مسیر تولید ثبت شود
        AiProvider provider = aiProvider.getIfAvailable();
        if (provider != null) {
            provider.generate(AiProvider.AiRequest.ofUserPrompt(
                    null,
                    "تولید آزمون از کتاب " + request.bookId()
                            + " صفحات " + request.pageFrom() + " تا " + request.pageTo()));
        }

        Exam exam = new Exam(request.classroomId(), request.title().trim());
        exam.setDescription(trimToNull(request.description()));
        exam.setDurationMinutes(request.durationMinutes());
        exam.setTotalScore(request.totalScore());
        exam.setBookId(request.bookId());
        exam.setStatus(ExamStatus.DRAFT);
        exam = examRepository.save(exam);

        List<ExamQuestion> created = new ArrayList<>();
        int order = 0;
        for (int i = 1; i <= mcqCount; i++) {
            ExamQuestion q = new ExamQuestion(exam.getId(), QuestionType.MCQ,
                    "سؤال چهارگزینه‌ای " + i + " (صفحات "
                            + request.pageFrom() + "–" + request.pageTo() + ")");
            q.setOptions("[\"گزینه الف\",\"گزینه ب\",\"گزینه ج\",\"گزینه د\"]");
            q.setCorrectAnswer("گزینه الف");
            q.setScore(BigDecimal.ONE);
            q.setDifficulty(i == 1 ? "EASY" : (i == mcqCount ? "HARD" : "MEDIUM"));
            q.setOrderIndex(order++);
            q.setAiGenerated(true);
            created.add(questionRepository.save(q));
        }

        for (int i = 1; i <= trueFalseCount; i++) {
            ExamQuestion tf = new ExamQuestion(exam.getId(), QuestionType.TRUE_FALSE,
                    "سؤال درست/نادرست " + i + " (صفحات "
                            + request.pageFrom() + "–" + request.pageTo() + ")");
            tf.setOptions("[\"درست\",\"نادرست\"]");
            tf.setCorrectAnswer("درست");
            tf.setScore(BigDecimal.ONE);
            tf.setDifficulty("EASY");
            tf.setOrderIndex(order++);
            tf.setAiGenerated(true);
            created.add(questionRepository.save(tf));
        }

        ExamQuestion essay = new ExamQuestion(exam.getId(), QuestionType.ESSAY,
                "سؤال تشریحی درباره محتوای صفحات "
                        + request.pageFrom() + " تا " + request.pageTo());
        essay.setScore(BigDecimal.valueOf(5));
        essay.setDifficulty("MEDIUM");
        essay.setOrderIndex(order);
        essay.setAiGenerated(true);
        created.add(questionRepository.save(essay));

        ExamBlueprint blueprint = new ExamBlueprint(
                exam.getId(), request.bookId(), request.pageFrom(), request.pageTo());
        blueprint.setDifficultyDistribution(trimToNull(request.difficultyDistribution()));
        String defaultDistribution = "{\"MCQ\":" + mcqCount
                + ",\"TRUE_FALSE\":" + trueFalseCount + ",\"ESSAY\":1}";
        blueprint.setQuestionTypeDistribution(trimToNull(request.questionTypeDistribution()) != null
                ? trimToNull(request.questionTypeDistribution())
                : defaultDistribution);
        blueprint.setQuestionCount(created.size());
        blueprintRepository.save(blueprint);

        BigDecimal total = created.stream()
                .map(ExamQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (exam.getTotalScore() == null) {
            exam.setTotalScore(total);
        }

        return mapper.toResponse(exam, created);
    }

    @Transactional
    public ExamResponse update(UUID id, UpdateExamRequest request) {
        Exam exam = requireExam(id);
        exam.setTitle(request.title().trim());
        exam.setDescription(trimToNull(request.description()));
        exam.setDurationMinutes(request.durationMinutes());
        exam.setTotalScore(request.totalScore());
        exam.setStartAt(request.startAt());
        exam.setEndAt(request.endAt());
        exam.setBookId(request.bookId());
        if (request.status() != null) {
            exam.setStatus(request.status());
        }
        if (request.shuffleQuestions() != null) {
            exam.setShuffleQuestions(request.shuffleQuestions());
        }
        if (request.showResultsImmediately() != null) {
            exam.setShowResultsImmediately(request.showResultsImmediately());
        }
        return mapper.toResponse(exam, questionRepository.findByExamIdOrderByOrderIndexAsc(id));
    }

    @Transactional
    public void delete(UUID id) {
        requireExam(id).softDelete();
    }

    @Transactional
    public ExamQuestionResponse addQuestion(UUID examId, CreateExamQuestionRequest request) {
        Exam exam = requireExam(examId);
        int nextOrder = request.orderIndex() != null
                ? request.orderIndex()
                : questionRepository.countByExamId(examId);

        ExamQuestion question = new ExamQuestion(exam.getId(), request.type(),
                request.questionText().trim());
        question.setOptions(trimToNull(request.options()));
        question.setCorrectAnswer(trimToNull(request.correctAnswer()));
        question.setScore(request.score() != null ? request.score() : BigDecimal.ONE);
        question.setDifficulty(trimToNull(request.difficulty()));
        question.setExplanation(trimToNull(request.explanation()));
        question.setOrderIndex(nextOrder);
        return mapper.toQuestionResponse(questionRepository.save(question));
    }

    @Transactional
    public ExamQuestionResponse updateQuestion(UUID examId, UUID questionId,
                                               CreateExamQuestionRequest request) {
        requireExam(examId);
        ExamQuestion question = questionRepository.findByIdAndExamId(questionId, examId)
                .orElseThrow(ResourceNotFoundException::generic);
        tenantGuard.requireOwned(question, "ExamQuestion");

        question.setType(request.type());
        question.setQuestionText(request.questionText().trim());
        question.setOptions(trimToNull(request.options()));
        question.setCorrectAnswer(trimToNull(request.correctAnswer()));
        if (request.score() != null) {
            question.setScore(request.score());
        }
        question.setDifficulty(trimToNull(request.difficulty()));
        question.setExplanation(trimToNull(request.explanation()));
        if (request.orderIndex() != null) {
            question.setOrderIndex(request.orderIndex());
        }
        return mapper.toQuestionResponse(question);
    }

    @Transactional
    public void deleteQuestion(UUID examId, UUID questionId) {
        requireExam(examId);
        ExamQuestion question = questionRepository.findByIdAndExamId(questionId, examId)
                .orElseThrow(ResourceNotFoundException::generic);
        tenantGuard.requireOwned(question, "ExamQuestion");
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public ExamBlueprintResponse getBlueprint(UUID examId) {
        requireExam(examId);
        List<ExamBlueprint> blueprints =
                blueprintRepository.findByExamIdOrderByCreatedAtDesc(examId);
        if (blueprints.isEmpty()) {
            throw ResourceNotFoundException.generic();
        }
        return toBlueprintResponse(blueprints.getFirst());
    }

    @Transactional
    public ExamResponse publish(UUID examId, PublishExamRequest request) {
        Exam exam = requireExam(examId);
        if (questionRepository.countByExamId(examId) == 0) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "آزمون بدون سؤال قابل انتشار نیست.");
        }
        exam.setStatus(ExamStatus.ACTIVE);
        examRepository.save(exam);

        String chatId = request != null ? trimToNull(request.eitaaChatId()) : null;
        if (chatId != null) {
            String text = buildPublishMessage(exam);
            EitaaSendResult sendResult = eitaaAdapter.sendToChat(null, chatId, text, null);
            if (!sendResult.success()) {
                log.warn("ارسال آزمون {} به ایتا شکست خورد: code={}",
                        exam.getId(), sendResult.errorCode());
            }
        }

        return mapper.toResponse(exam, questionRepository.findByExamIdOrderByOrderIndexAsc(examId));
    }

    @Transactional(readOnly = true)
    public List<ExamAttemptResponse> listAttempts(UUID examId) {
        requireExam(examId);
        return attemptRepository.findByExamIdOrderByStartedAtDesc(examId).stream()
                .map(a -> mapper.toAttemptResponse(a, answerRepository.findByAttemptId(a.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamResultsResponse results(UUID examId) {
        Exam exam = requireExam(examId);
        List<ExamAttemptResponse> attempts = listAttempts(examId);
        BigDecimal avg = BigDecimal.ZERO;
        int graded = 0;
        for (ExamAttemptResponse a : attempts) {
            if (a.score() != null) {
                avg = avg.add(a.score());
                graded++;
            }
        }
        if (graded > 0) {
            avg = avg.divide(BigDecimal.valueOf(graded), 2, RoundingMode.HALF_UP);
        }
        return new ExamResultsResponse(exam.getId(), exam.getTitle(), attempts.size(), avg, attempts);
    }

    @Transactional
    public ExamAttemptResponse gradeAttempt(UUID examId, UUID attemptId,
                                            GradeAttemptRequest request) {
        requireExam(examId);
        ExamAttempt attempt = attemptRepository.findById(attemptId).orElse(null);
        tenantGuard.requireOwned(attempt, "ExamAttempt");
        if (!Objects.equals(attempt.getExamId(), examId)) {
            throw ResourceNotFoundException.generic();
        }

        List<ExamAnswer> answers = answerRepository.findByAttemptId(attemptId);
        for (GradeAttemptRequest.AnswerGradeOverride override : request.overrides()) {
            ExamAnswer answer = answers.stream()
                    .filter(a -> a.getQuestionId().equals(override.questionId()))
                    .findFirst()
                    .orElseGet(() -> {
                        ExamAnswer created = new ExamAnswer(attemptId, override.questionId());
                        answers.add(created);
                        return created;
                    });
            answer.setAwardedScore(override.awardedScore());
            if (override.correct() != null) {
                answer.setCorrect(override.correct());
            }
            if (override.feedback() != null) {
                answer.setAiFeedback(trimToNull(override.feedback()));
            }
            answer.setAiGraded(false);
            answerRepository.save(answer);
        }

        BigDecimal total = answerRepository.findByAttemptId(attemptId).stream()
                .map(ExamAnswer::getAwardedScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        attempt.setScore(total);
        attempt.setStatus(AttemptStatus.GRADED);
        return mapper.toAttemptResponse(attempt, answerRepository.findByAttemptId(attemptId));
    }

    @Transactional(readOnly = true)
    public ExamAttemptReportResponse report(UUID attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId).orElse(null);
        tenantGuard.requireOwned(attempt, "ExamAttempt");
        Exam exam = requireExam(attempt.getExamId());
        List<ExamQuestion> questions =
                questionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
        List<ExamAnswer> answers = answerRepository.findByAttemptId(attemptId);
        return new ExamAttemptReportResponse(
                attempt.getId(),
                exam.getId(),
                exam.getTitle(),
                attempt.getStudentId(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getScore(),
                exam.getTotalScore(),
                attempt.getStatus(),
                exam.getDurationMinutes(),
                answers.stream().map(mapper::toAnswerResponse).toList(),
                questions.stream().map(mapper::toQuestionResponse).toList());
    }

    // ─────────────────────────── دانش‌آموز ───────────────────────────

    @Transactional
    public ExamAttemptResponse start(UUID examId, StartExamRequest request) {
        Exam exam = requireExam(examId);
        if (exam.getStatus() != ExamStatus.ACTIVE && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    "آزمون هنوز برای شرکت باز نیست.");
        }

        UUID studentId = request.studentId();
        attemptRepository.findByExamIdAndStudentIdAndStatus(
                        examId, studentId, AttemptStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                            "تلاش ناتمام برای این آزمون وجود دارد.");
                });

        int next = attemptRepository.maxAttemptNumber(examId, studentId) + 1;
        ExamAttempt attempt = attemptRepository.save(new ExamAttempt(examId, studentId, next));
        return mapper.toAttemptResponse(attempt, List.of());
    }

    @Transactional
    public ExamAnswerResponse saveAnswer(UUID attemptId, UUID studentId,
                                         SubmitAnswerRequest request) {
        ExamAttempt attempt = requireOwnAttempt(attemptId, studentId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    "این تلاش دیگر قابل ویرایش نیست.");
        }
        rejectIfPastDeadline(attempt);

        ExamQuestion question = questionRepository
                .findByIdAndExamId(request.questionId(), attempt.getExamId())
                .orElseThrow(ResourceNotFoundException::generic);
        tenantGuard.requireOwned(question, "ExamQuestion");

        ExamAnswer answer = answerRepository
                .findByAttemptIdAndQuestionId(attemptId, request.questionId())
                .orElseGet(() -> new ExamAnswer(attemptId, request.questionId()));
        answer.setAnswerText(trimToNull(request.answerText()));
        answer.setSelectedOptions(trimToNull(request.selectedOptions()));
        return mapper.toAnswerResponse(answerRepository.save(answer));
    }

    @Transactional
    public ExamAttemptResponse submit(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = requireOwnAttempt(attemptId, studentId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "این تلاش قبلاً ارسال شده است.");
        }
        rejectIfPastDeadline(attempt);

        Exam exam = requireExam(attempt.getExamId());
        List<ExamQuestion> questions =
                questionRepository.findByExamIdOrderByOrderIndexAsc(attempt.getExamId());
        List<ExamAnswer> answers = answerRepository.findByAttemptId(attemptId);

        BigDecimal total = BigDecimal.ZERO;
        for (ExamQuestion question : questions) {
            ExamAnswer answer = answers.stream()
                    .filter(a -> a.getQuestionId().equals(question.getId()))
                    .findFirst()
                    .orElse(null);
            if (answer == null) {
                continue;
            }
            autoGrade(question, answer);
            aiGradeIfNeeded(question, answer);
            if (answer.getAwardedScore() != null) {
                total = total.add(answer.getAwardedScore());
            }
            answerRepository.save(answer);
        }

        attempt.setScore(total);
        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus(AttemptStatus.GRADED);

        UUID tenantId = TenantContext.requireTenantId();
        events.publishEvent(new ExamCompletedEvent(
                tenantId, exam.getId(), attempt.getId(), studentId, total, exam.getTotalScore()));

        if (exam.getTotalScore() != null && exam.getTotalScore().signum() > 0) {
            BigDecimal percent = total.multiply(BigDecimal.valueOf(100))
                    .divide(exam.getTotalScore(), 2, RoundingMode.HALF_UP);
            if (percent.compareTo(SCORE_THRESHOLD_PERCENT) < 0) {
                events.publishEvent(new ScoreBelowThresholdEvent(
                        tenantId, exam.getId(), attempt.getId(), studentId,
                        total, exam.getTotalScore(), percent));
            }
        }

        return mapper.toAttemptResponse(attempt, answerRepository.findByAttemptId(attemptId));
    }

    @Transactional(readOnly = true)
    public ExamAttemptResponse getAttemptForStudent(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = requireOwnAttempt(attemptId, studentId);
        return mapper.toAttemptResponse(attempt, answerRepository.findByAttemptId(attemptId));
    }

    // ─────────────────────────── داخلی ───────────────────────────

    private void rejectIfPastDeadline(ExamAttempt attempt) {
        Exam exam = examRepository.findByIdAndDeletedAtIsNull(attempt.getExamId()).orElse(null);
        if (exam == null || exam.getDurationMinutes() == null) {
            return;
        }
        Instant deadline = attempt.getStartedAt().plus(Duration.ofMinutes(exam.getDurationMinutes()));
        if (Instant.now().isAfter(deadline)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    "مهلت آزمون به پایان رسیده است؛ ارسال پذیرفته نمی‌شود.");
        }
    }

    private void autoGrade(ExamQuestion question, ExamAnswer answer) {
        if (question.getType() != QuestionType.MCQ
                && question.getType() != QuestionType.TRUE_FALSE) {
            return;
        }
        String expected = normalize(question.getCorrectAnswer());
        String given = normalize(answer.getAnswerText());
        if (given == null) {
            given = normalize(answer.getSelectedOptions());
        }
        boolean correct = expected != null && expected.equals(given);
        answer.setCorrect(correct);
        answer.setAwardedScore(correct ? question.getScore() : BigDecimal.ZERO);
        answer.setAiGraded(false);
    }

    private void aiGradeIfNeeded(ExamQuestion question, ExamAnswer answer) {
        if (question.getType() != QuestionType.SHORT_ANSWER
                && question.getType() != QuestionType.ESSAY) {
            return;
        }
        AiProvider provider = aiProvider.getIfAvailable();
        if (provider == null) {
            return;
        }
        String prompt = "تصحیح سؤال "
                + question.getType()
                + ": "
                + question.getQuestionText()
                + " | پاسخ: "
                + Optional.ofNullable(answer.getAnswerText()).orElse("");
        AiProvider.AiStructuredResponse<AiEssayGradeResult> structured =
                provider.generateStructured(
                        AiProvider.AiRequest.ofUserPrompt(null, prompt),
                        AiEssayGradeResult.class);
        AiEssayGradeResult grade = structured.parsed();
        if (grade == null) {
            return;
        }
        BigDecimal awarded = grade.awardedScore() != null
                ? grade.awardedScore().min(question.getScore())
                : question.getScore();
        if (awarded.signum() < 0) {
            awarded = BigDecimal.ZERO;
        }
        answer.setAwardedScore(awarded);
        answer.setCorrect(grade.correct());
        answer.setAiFeedback(trimToNull(grade.feedback()) != null
                ? grade.feedback()
                : "تصحیح خودکار هوش مصنوعی");
        answer.setAiGraded(true);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1)
                    .replace("\"", "")
                    .trim();
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private Exam requireExam(UUID id) {
        Exam exam = examRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        return tenantGuard.requireOwned(exam, "Exam");
    }

    private ExamAttempt requireOwnAttempt(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId).orElse(null);
        tenantGuard.requireOwned(attempt, "ExamAttempt");
        if (!Objects.equals(attempt.getStudentId(), studentId)) {
            throw ResourceNotFoundException.generic();
        }
        return attempt;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ExamBlueprintResponse toBlueprintResponse(ExamBlueprint blueprint) {
        return new ExamBlueprintResponse(
                blueprint.getId(),
                blueprint.getExamId(),
                blueprint.getBookId(),
                blueprint.getPageFrom(),
                blueprint.getPageTo(),
                blueprint.getDifficultyDistribution(),
                blueprint.getQuestionTypeDistribution(),
                blueprint.getQuestionCount(),
                blueprint.getAiUsageId(),
                blueprint.getCreatedAt());
    }

    private static String buildPublishMessage(Exam exam) {
        StringBuilder sb = new StringBuilder();
        sb.append("آزمون تازه: ").append(exam.getTitle());
        if (exam.getDescription() != null && !exam.getDescription().isBlank()) {
            sb.append('\n').append(exam.getDescription());
        }
        sb.append("\n\nباز کردن در برنامک: exm_").append(exam.getId());
        return sb.toString();
    }
}
