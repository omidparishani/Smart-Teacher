package ir.omid.smartteacher.classroom.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ir.omid.smartteacher.classroom.internal.api.dto.ClassStudentResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.EnrollStudentRequest;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoom;
import ir.omid.smartteacher.classroom.internal.domain.ClassStudent;
import ir.omid.smartteacher.classroom.internal.domain.ClassStudentRepository;
import ir.omid.smartteacher.classroom.internal.domain.SubjectRepository;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.student.StudentDirectory;
import ir.omid.smartteacher.student.StudentDirectory.StudentSummary;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * آزمون قاعده دسترسی و ظرفیت ثبت‌نام — بدون دیتابیس.
 *
 * <p>آنچه اینجا ثابت می‌شود و تست جداسازی مستأجر ثابت نمی‌کند: معلم
 * همکار نمی‌تواند در کلاس دیگری ثبت‌نام کند (چون {@code requireAccessible}
 * صدا زده می‌شود)، ظرفیت پر رد می‌شود، و دانش‌آموز ناموجود ۴۰۴ می‌گیرد.
 */
class EnrollmentServiceAccessTest {

    private static final UUID CLASS_ID = UUID.fromString("99999999-9999-7999-8999-999999999999");
    private static final UUID STUDENT_ID = UUID.fromString("aaaaaaaa-aaaa-7aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-7111-8111-111111111111");

    private ClassRoomService classRoomService;
    private ClassStudentRepository classStudentRepository;
    private StudentDirectory studentDirectory;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        classRoomService = mock(ClassRoomService.class);
        classStudentRepository = mock(ClassStudentRepository.class);
        studentDirectory = mock(StudentDirectory.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);

        service = new EnrollmentService(
                classRoomService,
                classStudentRepository,
                subjectRepository,
                studentDirectory);
    }

    private ClassRoom roomWithCapacity(Integer capacity) {
        ClassRoom room = new ClassRoom("پنجم الف");
        room.setCapacity(capacity);
        ReflectionTestUtils.setField(room, "id", CLASS_ID);
        ReflectionTestUtils.setField(room, "tenantId", TENANT_ID);
        return room;
    }

    @Test
    @DisplayName("ثبت‌نام، ابتدا دسترسی به کلاس را بررسی می‌کند")
    void enrollChecksClassAccessFirst() {
        when(classRoomService.requireAccessible(CLASS_ID))
                .thenThrow(ResourceNotFoundException.generic());

        assertThatThrownBy(() -> service.enroll(CLASS_ID,
                new EnrollStudentRequest(STUDENT_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(studentDirectory, never()).activeIdsAmong(any());
        verify(classStudentRepository, never()).save(any());
    }

    @Test
    @DisplayName("دانش‌آموز ناموجود یا حذف‌شده → ۴۰۴")
    void enrollRejectsUnknownStudent() {
        when(classRoomService.requireAccessible(CLASS_ID)).thenReturn(roomWithCapacity(30));
        when(studentDirectory.activeIdsAmong(List.of(STUDENT_ID))).thenReturn(Set.of());

        assertThatThrownBy(() -> service.enroll(CLASS_ID,
                new EnrollStudentRequest(STUDENT_ID, "۱")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(classStudentRepository, never()).save(any());
    }

    @Test
    @DisplayName("ثبت‌نام تکراری → ۴۰۹")
    void enrollRejectsDuplicate() {
        when(classRoomService.requireAccessible(CLASS_ID)).thenReturn(roomWithCapacity(30));
        when(studentDirectory.activeIdsAmong(List.of(STUDENT_ID))).thenReturn(Set.of(STUDENT_ID));
        when(classStudentRepository.existsByClassroomIdAndStudentId(CLASS_ID, STUDENT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.enroll(CLASS_ID,
                new EnrollStudentRequest(STUDENT_ID, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("ظرفیت پر → ۴۰۹ تعارض")
    void enrollRejectsWhenCapacityFull() {
        when(classRoomService.requireAccessible(CLASS_ID)).thenReturn(roomWithCapacity(1));
        when(studentDirectory.activeIdsAmong(List.of(STUDENT_ID))).thenReturn(Set.of(STUDENT_ID));
        when(classStudentRepository.existsByClassroomIdAndStudentId(CLASS_ID, STUDENT_ID))
                .thenReturn(false);

        // یک ثبت‌نام قبلی که دانش‌آموزش هنوز فعال است → ظرفیت ۱ پر است
        ClassStudent existing = new ClassStudent(CLASS_ID, UUID.randomUUID());
        when(classStudentRepository.findByClassroomIdOrderByEnrolledAtAsc(CLASS_ID))
                .thenReturn(List.of(existing));
        when(studentDirectory.summariesOf(any()))
                .thenReturn(Map.of(existing.getStudentId(),
                        new StudentSummary(existing.getStudentId(), "علی", null)));

        assertThatThrownBy(() -> service.enroll(CLASS_ID,
                new EnrollStudentRequest(STUDENT_ID, null)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
    }

    @Test
    @DisplayName("ثبت‌نام موفق، خلاصه دانش‌آموز را برمی‌گرداند")
    void enrollSucceedsAndReturnsSummary() {
        when(classRoomService.requireAccessible(CLASS_ID)).thenReturn(roomWithCapacity(null));
        when(studentDirectory.activeIdsAmong(List.of(STUDENT_ID))).thenReturn(Set.of(STUDENT_ID));
        when(classStudentRepository.existsByClassroomIdAndStudentId(CLASS_ID, STUDENT_ID))
                .thenReturn(false);
        when(classStudentRepository.findByClassroomIdOrderByEnrolledAtAsc(CLASS_ID))
                .thenReturn(List.of());
        when(classStudentRepository.save(any(ClassStudent.class))).thenAnswer(inv -> {
            ClassStudent cs = inv.getArgument(0);
            ReflectionTestUtils.setField(cs, "enrolledAt", java.time.Instant.parse("2026-01-01T00:00:00Z"));
            return cs;
        });
        when(studentDirectory.summariesOf(List.of(STUDENT_ID)))
                .thenReturn(Map.of(STUDENT_ID, new StudentSummary(STUDENT_ID, "سارا محمدی", "0123456789")));

        ClassStudentResponse response = service.enroll(CLASS_ID,
                new EnrollStudentRequest(STUDENT_ID, "۵"));

        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.fullName()).isEqualTo("سارا محمدی");
        assertThat(response.nationalCode()).isEqualTo("0123456789");
        assertThat(response.rollNumber()).isEqualTo("۵");
        verify(classStudentRepository).save(any(ClassStudent.class));
    }

    @Test
    @DisplayName("لغو ثبت‌نام ناموجود → ۴۰۴")
    void unenrollMissingReturns404() {
        when(classRoomService.requireAccessible(CLASS_ID)).thenReturn(roomWithCapacity(null));
        when(classStudentRepository.findByClassroomIdAndStudentId(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unenroll(CLASS_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(classStudentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("داشبورد هم دسترسی کلاس را الزام می‌کند")
    void dashboardChecksAccess() {
        when(classRoomService.requireAccessible(eq(CLASS_ID)))
                .thenThrow(ResourceNotFoundException.generic());

        assertThatThrownBy(() -> service.dashboard(CLASS_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
