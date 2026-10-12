package ir.omid.smartteacher.classroom.internal.application;

import ir.omid.smartteacher.classroom.ClassRoomAccess;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoom;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomRepository;
import ir.omid.smartteacher.classroom.internal.domain.ClassStudentRepository;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * پیاده‌سازی {@link ClassRoomAccess} — delegating به {@link ClassRoomService}.
 *
 * <p>قاعده دسترسی را دوباره نمی‌نویسد؛ همان {@code requireAccessible} را
 * صدا می‌زند تا روزی یکی عوض شود و دیگری فراموش نشود.
 */
@Service
public class ClassRoomAccessImpl implements ClassRoomAccess {

    private final ClassRoomService classRoomService;
    private final ClassRoomRepository classRoomRepository;
    private final ClassStudentRepository classStudentRepository;

    public ClassRoomAccessImpl(ClassRoomService classRoomService,
                               ClassRoomRepository classRoomRepository,
                               ClassStudentRepository classStudentRepository) {
        this.classRoomService = classRoomService;
        this.classRoomRepository = classRoomRepository;
        this.classStudentRepository = classStudentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ClassRoomRef requireAccessible(UUID classroomId) {
        ClassRoom room = classRoomService.requireAccessible(classroomId);
        return new ClassRoomRef(room.getId(), room.getName(), room.getHomeroomTeacherId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listAccessibleClassroomIds() {
        UUID teacherFilter = canManageAllClasses() ? null : TenantContext.requireUserId();
        return classRoomRepository.findAccessibleIds(teacherFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(UUID classroomId, UUID studentId) {
        return classStudentRepository.existsByClassroomIdAndStudentId(classroomId, studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> namesOf(Collection<UUID> classroomIds) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return Map.of();
        }
        return classRoomRepository.findByIdInAndDeletedAtIsNull(classroomIds).stream()
                .collect(Collectors.toMap(ClassRoom::getId, ClassRoom::getName));
    }

    private boolean canManageAllClasses() {
        TenantPrincipal principal = TenantContext.requirePrincipal();
        return principal.hasTenantRole(MemberRole.OWNER)
                || principal.hasTenantRole(MemberRole.SCHOOL_ADMIN);
    }
}
