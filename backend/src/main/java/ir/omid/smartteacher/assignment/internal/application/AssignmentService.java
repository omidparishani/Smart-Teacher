package ir.omid.smartteacher.assignment.internal.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.omid.smartteacher.ai.ContentAnalysisAi;
import ir.omid.smartteacher.ai.ContentAnalysisAi.SubmissionGrade;
import ir.omid.smartteacher.billing.UsageMetering;
import ir.omid.smartteacher.assignment.internal.api.dto.AssignmentResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.CreateAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.CreateSubmissionRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.EvaluationResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.PublishAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.ReviewSubmissionRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.SubmissionResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.TeacherReviewResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.UpdateAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.mapper.AssignmentMapper;
import ir.omid.smartteacher.assignment.internal.domain.AiEvaluation;
import ir.omid.smartteacher.assignment.internal.domain.AiEvaluationRepository;
import ir.omid.smartteacher.assignment.internal.domain.Assignment;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentRepository;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentStatus;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentSubmission;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentSubmissionRepository;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentType;
import ir.omid.smartteacher.assignment.internal.domain.EitaaSendStatus;
import ir.omid.smartteacher.assignment.internal.domain.EvaluationStatus;
import ir.omid.smartteacher.assignment.internal.domain.ReviewAction;
import ir.omid.smartteacher.assignment.internal.domain.SubmissionStatus;
import ir.omid.smartteacher.assignment.internal.domain.TeacherReview;
import ir.omid.smartteacher.assignment.internal.domain.TeacherReviewRepository;
import ir.omid.smartteacher.classroom.ClassRoomAccess;
import ir.omid.smartteacher.classroom.ClassRoomAccess.ClassRoomRef;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.api.SearchPattern;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.event.AssignmentPublishedEvent;
import ir.omid.smartteacher.common.event.SubmissionEvaluatedEvent;
import ir.omid.smartteacher.eitaa.EitaaAdapter;
import ir.omid.smartteacher.eitaa.EitaaIdentity;
import ir.omid.smartteacher.eitaa.EitaaIdentityRepository;
import ir.omid.smartteacher.eitaa.EitaaNotification;
import ir.omid.smartteacher.eitaa.EitaaSendResult;
import ir.omid.smartteacher.security.TenantGuard;
import ir.omid.smartteacher.student.StudentDirectory;
import ir.omid.smartteacher.student.StudentDirectory.StudentSummary;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * منطق کسب‌وکار تکلیف — گام ۱ فاز ۷.
 *
 * <p><b>🔴 قاعده دسترسی معلم:</b> فقط تکالیف کلاس‌هایی که
 * {@link ClassRoomAccess#requireAccessible} اجازه می‌دهد.
 *
 * <p><b>🔴 انتشار و ایتا:</b> وضعیت {@code PUBLISHED} ذخیره می‌شود؛ سپس
 * در همان تراکنش {@link EitaaAdapter#sendToChat} صدا زده می‌شود.
 * شکست ارسال <b>استثنا نیست</b> — پس rollback نمی‌شود
 * ({@code docs/eitaa.md} بخش ۹ و {@code SendFailureDoesNotRollbackAssignmentTest}).
 *
 * <p><b>🔴 دریافت از چت ایتا نیست.</b> پاسخ فقط از Mini App ثبت می‌شود.
 */
@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("title", "dueAt", "status", "createdAt", "updatedAt");

    private static final String DEFAULT_SORT = "createdAt,desc";

    private static final BigDecimal REVIEW_CONFIDENCE_THRESHOLD = new BigDecimal("0.70");

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final TeacherReviewRepository teacherReviewRepository;
    private final AssignmentMapper mapper;
    private final ClassRoomAccess classRoomAccess;
    private final StudentDirectory studentDirectory;
    private final TenantGuard tenantGuard;
    private final EitaaAdapter eitaaAdapter;
    private final EitaaIdentityRepository eitaaIdentityRepository;
    private final ContentAnalysisAi contentAnalysisAi;
    private final ObjectProvider<UsageMetering> usageMetering;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationEventPublisher events;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             AssignmentSubmissionRepository submissionRepository,
                             AiEvaluationRepository aiEvaluationRepository,
                             TeacherReviewRepository teacherReviewRepository,
                             AssignmentMapper mapper,
                             ClassRoomAccess classRoomAccess,
                             StudentDirectory studentDirectory,
                             TenantGuard tenantGuard,
                             EitaaAdapter eitaaAdapter,
                             EitaaIdentityRepository eitaaIdentityRepository,
                             ContentAnalysisAi contentAnalysisAi,
                             ObjectProvider<UsageMetering> usageMetering,
                             ApplicationEventPublisher events) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.aiEvaluationRepository = aiEvaluationRepository;
        this.teacherReviewRepository = teacherReviewRepository;
        this.mapper = mapper;
        this.classRoomAccess = classRoomAccess;
        this.studentDirectory = studentDirectory;
        this.tenantGuard = tenantGuard;
        this.eitaaAdapter = eitaaAdapter;
        this.eitaaIdentityRepository = eitaaIdentityRepository;
        this.contentAnalysisAi = contentAnalysisAi;
        this.usageMetering = usageMetering;
        this.events = events;
    }

    // ─────────────────────────── تکلیف ───────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AssignmentResponse> list(int page, int size, String sort,
                                                 String search, AssignmentStatus status,
                                                 UUID classroomId) {
        List<UUID> accessible = classRoomAccess.listAccessibleClassroomIds();
        if (accessible.isEmpty()) {
            return PageResponse.empty(page, size);
        }

        if (classroomId != null && !accessible.contains(classroomId)) {
            // IDOR داخل مستأجر — ۴۰۴ یکسان
            throw ResourceNotFoundException.generic();
        }

        Pageable pageable = PageableFactory.of(page, size, sort, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<Assignment> result = assignmentRepository.findForListing(
                accessible, SearchPattern.like(search), status, classroomId, pageable);

        Map<UUID, String> names = classroomNames(result.getContent());
        return PageResponse.from(result, a -> toResponse(a, names.get(a.getClassroomId())));
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(UUID id) {
        Assignment assignment = tenantGuard.requireOwned(
                assignmentRepository.findByIdAndDeletedAtIsNull(id).orElse(null),
                "Assignment");

        if (isStaff()) {
            ClassRoomRef room = classRoomAccess.requireAccessible(assignment.getClassroomId());
            return toResponse(assignment, room.name());
        }

        // دانش‌آموز: فقط تکلیف منتشر/بسته‌شدهٔ مستأجر خودش
        if (assignment.getStatus() == AssignmentStatus.DRAFT
                || assignment.getStatus() == AssignmentStatus.ARCHIVED) {
            throw ResourceNotFoundException.generic();
        }
        String name = classRoomAccess.namesOf(List.of(assignment.getClassroomId()))
                .get(assignment.getClassroomId());
        return toResponse(assignment, name);
    }

    @Transactional
    public AssignmentResponse create(CreateAssignmentRequest request) {
        ClassRoomRef room = classRoomAccess.requireAccessible(request.classroomId());
        validatePages(request.pageFrom(), request.pageTo());

        Assignment assignment = new Assignment(request.title().trim(), room.id());
        assignment.setDescription(trimToNull(request.description()));
        assignment.setType(request.type() != null ? request.type() : AssignmentType.HOMEWORK);
        assignment.setMaxScore(request.maxScore());
        assignment.setDueAt(request.dueAt());
        assignment.setBookId(request.bookId());
        assignment.setPageFrom(request.pageFrom());
        assignment.setPageTo(request.pageTo());
        assignment.setAiEvaluationEnabled(
                Boolean.TRUE.equals(request.aiEvaluationEnabled()));
        assignment.setStatus(AssignmentStatus.DRAFT);

        try {
            Assignment saved = assignmentRepository.save(assignment);
            return toResponse(saved, room.name());
        } catch (DataIntegrityViolationException ex) {
            throw bookOrFkFailure(ex);
        }
    }

    @Transactional
    public AssignmentResponse update(UUID id, UpdateAssignmentRequest request) {
        Assignment assignment = requireAccessibleAssignment(id);
        ClassRoomRef room = classRoomAccess.requireAccessible(assignment.getClassroomId());
        validatePages(request.pageFrom(), request.pageTo());

        assignment.setTitle(request.title().trim());
        assignment.setDescription(trimToNull(request.description()));
        if (request.type() != null) {
            assignment.setType(request.type());
        }
        assignment.setMaxScore(request.maxScore());
        assignment.setDueAt(request.dueAt());
        assignment.setBookId(request.bookId());
        assignment.setPageFrom(request.pageFrom());
        assignment.setPageTo(request.pageTo());
        if (request.aiEvaluationEnabled() != null) {
            assignment.setAiEvaluationEnabled(request.aiEvaluationEnabled());
        }
        if (request.status() != null) {
            assignment.setStatus(request.status());
        }

        try {
            Assignment saved = assignmentRepository.save(assignment);
            return toResponse(saved, room.name());
        } catch (DataIntegrityViolationException ex) {
            throw bookOrFkFailure(ex);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Assignment assignment = requireAccessibleAssignment(id);
        assignment.softDelete();
        assignmentRepository.save(assignment);
    }

    /**
     * انتشار تکلیف و ارسال اختیاری به گروه ایتا.
     *
     * <p><b>🔴 ترتیب عمدی:</b> اول وضعیت {@code PUBLISHED} ذخیره می‌شود،
     * بعد ارسال. اگر ارسال شکست بخورد، نتیجه مقدار ناموفق است نه استثنا —
     * پس تراکنش برنمی‌گردد و تکلیف منتشرمانده می‌ماند.
     */
    @Transactional
    public AssignmentResponse publish(UUID id, PublishAssignmentRequest request) {
        Assignment assignment = requireAccessibleAssignment(id);
        ClassRoomRef room = classRoomAccess.requireAccessible(assignment.getClassroomId());

        assignment.setStatus(AssignmentStatus.PUBLISHED);

        String chatId = request != null ? trimToNull(request.eitaaChatId()) : null;
        if (chatId == null) {
            chatId = trimToNull(assignment.getEitaaChatId());
        }

        if (chatId == null) {
            assignment.setEitaaSendStatus(EitaaSendStatus.SKIPPED);
            assignment.setEitaaMessageId(null);
        } else {
            assignment.setEitaaChatId(chatId);
            String text = buildPublishMessage(assignment);
            // ⚠️ شکست ارسال استثنا نیست — قرارداد EitaaAdapter.
            EitaaSendResult sendResult = eitaaAdapter.sendToChat(null, chatId, text, null);
            if (sendResult.success()) {
                assignment.setEitaaSendStatus(EitaaSendStatus.SENT);
                assignment.setEitaaMessageId(sendResult.messageId());
            } else {
                assignment.setEitaaSendStatus(EitaaSendStatus.FAILED);
                assignment.setEitaaMessageId(null);
                log.warn("ارسال تکلیف {} به ایتا شکست خورد: code={}",
                        assignment.getId(), sendResult.errorCode());
            }
        }

        Assignment saved = assignmentRepository.save(assignment);
        events.publishEvent(new AssignmentPublishedEvent(
                TenantContext.requireTenantId(),
                saved.getId(),
                saved.getClassroomId(),
                saved.getTitle()));
        return toResponse(saved, room.name());
    }

    // ─────────────────────────── ارسال‌ها ───────────────────────────

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listSubmissions(UUID assignmentId) {
        Assignment assignment = requireAccessibleAssignment(assignmentId);
        List<AssignmentSubmission> submissions =
                submissionRepository.findByAssignmentIdOrderBySubmittedAtAsc(assignment.getId());

        Map<UUID, StudentSummary> names = studentDirectory.summariesOf(
                submissions.stream().map(AssignmentSubmission::getStudentId).toList());

        return submissions.stream()
                .map(s -> {
                    StudentSummary summary = names.get(s.getStudentId());
                    return mapper.toSubmissionResponse(
                            s, summary != null ? summary.fullName() : null);
                })
                .toList();
    }

    /**
     * ثبت یا به‌روزرسانی پاسخ متنی دانش‌آموز از Mini App.
     */
    @Transactional
    public SubmissionResponse submit(UUID assignmentId, CreateSubmissionRequest request) {
        Assignment assignment = requireAssignmentForSubmission(assignmentId);

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED
                && assignment.getStatus() != AssignmentStatus.CLOSED) {
            // معلم می‌تواند پیش‌نویس را هم ببیند؛ ولی دانش‌آموز فقط روی
            // منتشرشده ارسال می‌کند. معلم staff هم برای تست می‌تواند
            // روی DRAFT ارسال کند اگر کلاس را ببیند — ولی برای سادگی
            // فقط PUBLISHED/CLOSED.
            if (!isStaff()) {
                throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                        "این تکلیف هنوز برای ارسال پاسخ باز نیست.");
            }
        }

        Set<UUID> active = studentDirectory.activeIdsAmong(List.of(request.studentId()));
        if (!active.contains(request.studentId())) {
            throw ResourceNotFoundException.of("دانش‌آموز");
        }

        if (!classRoomAccess.isStudentEnrolled(assignment.getClassroomId(), request.studentId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "این دانش‌آموز در کلاس این تکلیف ثبت‌نام نشده است.");
        }

        // دانش‌آموز فقط برای «خودش» — بدون لینک user↔student، مالکیت از
        // submitted_by_user_id در خواندن اعمال می‌شود؛ اینجا اگر نقش
        // STUDENT باشد و قبلاً ارسال دیگری با user دیگر برای همین
        // studentId باشد، بازنویسی ممنوع نیست (گام ۱). محدودیت اصلی
        // در getSubmission است.

        AssignmentSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, request.studentId())
                .orElseGet(() -> new AssignmentSubmission(
                        assignmentId, request.studentId(), request.answerText().trim()));

        submission.setAnswerText(request.answerText().trim());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(Instant.now());
        submission.setSubmittedByUserId(TenantContext.requireUserId());

        try {
            AssignmentSubmission saved = submissionRepository.save(submission);
            StudentSummary summary = studentDirectory.summariesOf(List.of(saved.getStudentId()))
                    .get(saved.getStudentId());
            return mapper.toSubmissionResponse(
                    saved, summary != null ? summary.fullName() : null);
        } catch (DataIntegrityViolationException ex) {
            throw DuplicateResourceException.of("ارسال تکلیف", "دانش‌آموز");
        }
    }

    /**
     * یک ارسال مشخص — معلم: همه ارسال‌های کلاس خودش؛ دانش‌آموز: فقط مال خودش.
     */
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID submissionId) {
        AssignmentSubmission submission = tenantGuard.requireOwned(
                submissionRepository.findById(submissionId).orElse(null),
                "AssignmentSubmission");

        Assignment assignment = requireAssignmentForSubmission(submission.getAssignmentId());

        if (isStudentOnly()) {
            UUID actorId = TenantContext.requireUserId();
            if (!Objects.equals(submission.getSubmittedByUserId(), actorId)) {
                throw ResourceNotFoundException.generic();
            }
        } else {
            // معلم/مدیر — باید به کلاس تکلیف دسترسی داشته باشد
            classRoomAccess.requireAccessible(assignment.getClassroomId());
        }

        StudentSummary summary = studentDirectory.summariesOf(List.of(submission.getStudentId()))
                .get(submission.getStudentId());
        return mapper.toSubmissionResponse(
                submission, summary != null ? summary.fullName() : null);
    }

    // ─────────────────────────── تصحیح AI و بازبینی ───────────────────────────

    /**
     * تصحیح یک ارسال با Mock AI — یک ردیف {@code ai_evaluations} می‌سازد/به‌روز می‌کند.
     */
    @Transactional
    public EvaluationResponse evaluate(UUID assignmentId, UUID submissionId) {
        Assignment assignment = requireAccessibleAssignment(assignmentId);
        AssignmentSubmission submission = requireSubmissionOfAssignment(assignmentId, submissionId);

        consumeAiRequestQuota();

        SubmissionGrade grade = contentAnalysisAi.gradeSubmission(
                assignment.getTitle(),
                submission.getAnswerText(),
                assignment.getMaxScore());

        AiEvaluation evaluation = aiEvaluationRepository.findBySubmissionId(submissionId)
                .orElseGet(() -> new AiEvaluation(submissionId));
        applyGrade(evaluation, grade);
        AiEvaluation saved = aiEvaluationRepository.save(evaluation);

        submission.setStatus(SubmissionStatus.AI_EVALUATED);
        submissionRepository.save(submission);

        notifyQuietly(submission.getSubmittedByUserId(),
                "تکلیف «" + assignment.getTitle() + "» تصحیح شد. نمره پیشنهادی: "
                        + grade.score());
        notifyQuietly(TenantContext.requireUserId(),
                "تصحیح AI برای ارسال " + submissionId + " آماده است.");

        events.publishEvent(new SubmissionEvaluatedEvent(
                TenantContext.requireTenantId(),
                assignmentId,
                submissionId,
                assignment.getTitle(),
                grade.score()));

        return toEvaluationResponse(saved, submission.getFinalScore());
    }

    /**
     * بازبینی معلم — ردیف تازه در {@code teacher_reviews} (Append-Only).
     */
    @Transactional
    public TeacherReviewResponse review(UUID assignmentId,
                                        UUID submissionId,
                                        ReviewSubmissionRequest request) {
        Assignment assignment = requireAccessibleAssignment(assignmentId);
        AssignmentSubmission submission = requireSubmissionOfAssignment(assignmentId, submissionId);

        BigDecimal previous = submission.getFinalScore();
        if (previous == null) {
            previous = aiEvaluationRepository.findBySubmissionId(submissionId)
                    .map(AiEvaluation::getAiScore)
                    .orElse(null);
        }

        ReviewAction action = request.action();
        BigDecimal newScore = request.newScore();

        switch (action) {
            case ACCEPT -> {
                if (newScore == null) {
                    newScore = previous;
                }
                submission.setFinalScore(newScore);
                submission.setTeacherOverrodeAi(false);
                submission.setStatus(SubmissionStatus.REVIEWED);
            }
            case EDIT_SCORE -> {
                if (newScore == null) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED,
                            "برای ویرایش نمره، newScore الزامی است.");
                }
                submission.setFinalScore(newScore);
                submission.setTeacherOverrodeAi(true);
                submission.setStatus(SubmissionStatus.GRADED);
            }
            case EDIT_FEEDBACK -> {
                if (newScore != null) {
                    submission.setFinalScore(newScore);
                    submission.setTeacherOverrodeAi(true);
                }
                submission.setStatus(SubmissionStatus.REVIEWED);
            }
            case REJECT -> {
                submission.setFinalScore(null);
                submission.setStatus(SubmissionStatus.RETURNED);
            }
            case RE_EVALUATE -> {
                // فقط لاگ؛ تصحیح دوباره با evaluate جداگانه
                submission.setStatus(SubmissionStatus.SUBMITTED);
            }
            default -> throw new AppException(ErrorCode.VALIDATION_FAILED, "اقدام بازبینی نامعتبر است.");
        }

        submissionRepository.save(submission);

        TeacherReview review = new TeacherReview(
                submissionId,
                TenantContext.requireUserId(),
                action,
                previous,
                newScore,
                trimToNull(request.comment()));
        TeacherReview saved = teacherReviewRepository.save(review);

        notifyQuietly(submission.getSubmittedByUserId(),
                "بازبینی معلم برای تکلیف «" + assignment.getTitle() + "»: " + action.name());

        return toReviewResponse(saved);
    }

    /**
     * تصحیح همه ارسال‌های یک تکلیف.
     */
    @Transactional
    public List<EvaluationResponse> reevaluateAll(UUID assignmentId) {
        Assignment assignment = requireAccessibleAssignment(assignmentId);
        List<AssignmentSubmission> submissions =
                submissionRepository.findByAssignmentIdOrderBySubmittedAtAsc(assignment.getId());
        List<EvaluationResponse> results = new ArrayList<>();
        for (AssignmentSubmission submission : submissions) {
            results.add(evaluate(assignmentId, submission.getId()));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<TeacherReviewResponse> listReviews(UUID assignmentId, UUID submissionId) {
        requireAccessibleAssignment(assignmentId);
        requireSubmissionOfAssignment(assignmentId, submissionId);
        return teacherReviewRepository.findBySubmissionIdOrderByReviewedAtAsc(submissionId)
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getEvaluation(UUID assignmentId, UUID submissionId) {
        AssignmentSubmission submission = requireSubmissionReadable(assignmentId, submissionId);
        AiEvaluation evaluation = aiEvaluationRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> ResourceNotFoundException.of("ارزیابی AI"));
        return toEvaluationResponse(evaluation, submission.getFinalScore());
    }

    /**
     * ارسال قابل‌خواندن برای معلم (کلاس خودش) یا دانش‌آموز (فقط مال خودش).
     */
    private AssignmentSubmission requireSubmissionReadable(UUID assignmentId, UUID submissionId) {
        AssignmentSubmission submission = requireSubmissionOfAssignment(assignmentId, submissionId);
        Assignment assignment = requireAssignmentForSubmission(assignmentId);
        if (isStudentOnly()) {
            UUID actorId = TenantContext.requireUserId();
            if (!Objects.equals(submission.getSubmittedByUserId(), actorId)) {
                throw ResourceNotFoundException.generic();
            }
        } else {
            classRoomAccess.requireAccessible(assignment.getClassroomId());
        }
        return submission;
    }

    // ─────────────────────────── کمکی ───────────────────────────

    private AssignmentSubmission requireSubmissionOfAssignment(UUID assignmentId, UUID submissionId) {
        AssignmentSubmission submission = tenantGuard.requireOwned(
                submissionRepository.findById(submissionId).orElse(null),
                "AssignmentSubmission");
        if (!Objects.equals(submission.getAssignmentId(), assignmentId)) {
            throw ResourceNotFoundException.generic();
        }
        return submission;
    }

    private void applyGrade(AiEvaluation evaluation, SubmissionGrade grade) {
        evaluation.setAiScore(grade.score());
        evaluation.setCorrect(grade.correct());
        evaluation.setMistakes(toJson(grade.mistakes()));
        evaluation.setStrengths(toJson(grade.strengths()));
        evaluation.setWeaknesses(toJson(grade.weaknesses()));
        evaluation.setFeedback(grade.feedback());
        evaluation.setTeacherNote(grade.teacherNote());
        evaluation.setConfidence(grade.confidence());
        evaluation.setRawResponse(toJson(grade));
        if (grade.confidence() != null
                && grade.confidence().compareTo(REVIEW_CONFIDENCE_THRESHOLD) < 0) {
            evaluation.setStatus(EvaluationStatus.LOW_CONFIDENCE);
        } else {
            evaluation.setStatus(EvaluationStatus.SUCCESS);
        }
    }

    private EvaluationResponse toEvaluationResponse(AiEvaluation evaluation, BigDecimal finalScore) {
        BigDecimal confidence = evaluation.getConfidence();
        boolean requiresReview = confidence == null
                || confidence.compareTo(REVIEW_CONFIDENCE_THRESHOLD) < 0
                || evaluation.getStatus() == EvaluationStatus.LOW_CONFIDENCE;
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getSubmissionId(),
                evaluation.getStatus(),
                evaluation.getAiScore(),
                evaluation.getCorrect(),
                fromJsonList(evaluation.getMistakes()),
                fromJsonList(evaluation.getStrengths()),
                fromJsonList(evaluation.getWeaknesses()),
                evaluation.getFeedback(),
                evaluation.getTeacherNote(),
                confidence,
                requiresReview,
                finalScore);
    }

    private TeacherReviewResponse toReviewResponse(TeacherReview review) {
        return new TeacherReviewResponse(
                review.getId(),
                review.getSubmissionId(),
                review.getReviewerUserId(),
                review.getAction(),
                review.getPreviousScore(),
                review.getNewScore(),
                review.getComment(),
                review.getReviewedAt());
    }

    /**
     * اعلان اختیاری — فقط اگر write access شناخته‌شده باشد؛ شکست بی‌صداست.
     */
    private void notifyQuietly(UUID userId, String text) {
        if (userId == null || text == null || text.isBlank()) {
            return;
        }
        try {
            List<EitaaIdentity> identities = eitaaIdentityRepository.findByUserId(userId);
            for (EitaaIdentity identity : identities) {
                if (!identity.isAllowsWriteToPm()) {
                    continue;
                }
                EitaaSendResult result = eitaaAdapter.notifyUser(
                        identity.getEitaaUserId(), EitaaNotification.of(text));
                if (!result.success()) {
                    log.debug("اعلان ایتا برای کاربر {} ناموفق: {}", userId, result.errorCode());
                }
            }
        } catch (Exception ex) {
            // ⚠️ شکست اعلان هرگز تراکنش تصحیح/بازبینی را خراب نمی‌کند
            log.warn("اعلان ایتا نادیده گرفته شد: {}", ex.toString());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "سریال‌سازی JSON ناموفق بود.", null, ex);
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of(json);
        }
    }


    private Assignment requireAccessibleAssignment(UUID id) {
        Assignment assignment = tenantGuard.requireOwned(
                assignmentRepository.findByIdAndDeletedAtIsNull(id).orElse(null),
                "Assignment");
        classRoomAccess.requireAccessible(assignment.getClassroomId());
        return assignment;
    }

    /**
     * برای ارسال: دانش‌آموز نیازی به دسترسی «معلم کلاس» ندارد؛ فقط
     * تکلیف منتشرشدهٔ مستأجر خودش را می‌بیند. معلم همچنان از
     * requireAccessible رد می‌شود.
     */
    private Assignment requireAssignmentForSubmission(UUID id) {
        Assignment assignment = tenantGuard.requireOwned(
                assignmentRepository.findByIdAndDeletedAtIsNull(id).orElse(null),
                "Assignment");
        if (isStaff()) {
            classRoomAccess.requireAccessible(assignment.getClassroomId());
        }
        return assignment;
    }

    private AssignmentResponse toResponse(Assignment assignment, String classroomName) {
        long count = submissionRepository.countByAssignmentId(assignment.getId());
        return mapper.toResponse(assignment, classroomName, count);
    }

    private Map<UUID, String> classroomNames(List<Assignment> assignments) {
        return classRoomAccess.namesOf(
                assignments.stream().map(Assignment::getClassroomId).distinct().toList());
    }

    private static void validatePages(Integer pageFrom, Integer pageTo) {
        if (pageFrom == null && pageTo == null) {
            return;
        }
        if (pageFrom == null || pageTo == null || pageFrom <= 0 || pageTo < pageFrom) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "بازه صفحه نامعتبر است (هر دو لازم‌اند و pageTo ≥ pageFrom > ۰).");
        }
    }

    private void consumeAiRequestQuota() {
        UsageMetering metering = usageMetering.getIfAvailable();
        if (metering != null) {
            metering.consume(UsageMetering.METRIC_AI_REQUESTS, 1);
        }
    }

    private static String buildPublishMessage(Assignment assignment) {
        StringBuilder sb = new StringBuilder();
        sb.append("تکلیف تازه: ").append(assignment.getTitle());
        if (assignment.getDescription() != null && !assignment.getDescription().isBlank()) {
            sb.append('\n').append(assignment.getDescription());
        }
        // Deep Link start_param — دانش‌آموز با کلیک وارد Mini App می‌شود
        sb.append("\n\nباز کردن در برنامک: asg_").append(assignment.getId());
        return sb.toString();
    }

    private static boolean isStaff() {
        TenantPrincipal p = TenantContext.requirePrincipal();
        return p.hasTenantRole(MemberRole.OWNER)
                || p.hasTenantRole(MemberRole.SCHOOL_ADMIN)
                || p.hasTenantRole(MemberRole.TEACHER)
                || p.hasTenantRole(MemberRole.ASSISTANT);
    }

    private static boolean isStudentOnly() {
        TenantPrincipal p = TenantContext.requirePrincipal();
        return p.hasTenantRole(MemberRole.STUDENT) && !isStaff();
    }

    private static AppException bookOrFkFailure(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        if (msg != null && msg.contains("fk_assignments_book")) {
            throw ResourceNotFoundException.of("کتاب");
        }
        throw new AppException(ErrorCode.INVALID_REQUEST, "ارجاع نامعتبر در تکلیف.");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
