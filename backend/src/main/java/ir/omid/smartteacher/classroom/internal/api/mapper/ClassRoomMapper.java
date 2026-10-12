package ir.omid.smartteacher.classroom.internal.api.mapper;

import ir.omid.smartteacher.classroom.internal.api.dto.ClassRoomResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.SubjectResponse;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoom;
import ir.omid.smartteacher.classroom.internal.domain.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * نگاشت موجودیت‌های دامنه به DTOهای وب — با MapStruct.
 *
 * <p>پیاده‌سازی این اینترفیس در زمان کامپایل ساخته می‌شود
 * ({@code ClassRoomMapperImpl}) و به‌عنوان Bean در دسترس است. پس هیچ
 * کد نگاشتی دستی نوشته نمی‌شود که با افزودن یک فیلد تازه به موجودیت،
 * بی‌صدا از قلم بیفتد.
 *
 * <p><b>⚠️ چرا یک متد {@code default} هم اینجا هست؟</b> چون نام درس در
 * {@link ClassRoom} وجود ندارد — یک ستون {@code UUID} به جدول دیگری
 * است. سرویس نام را در یک کوئری دسته‌ای می‌گیرد و به این متد می‌دهد.
 * اگر این کار را به MapStruct می‌سپردیم، به یک {@code @Context} و
 * پرس‌وجوی تک‌تک نیاز داشت که همان N+1 است. نگاشت تک‌تک فیلدها همچنان
 * کار MapStruct است؛ فقط {@code subjectName} از بیرون می‌آید.
 */
@Mapper(componentModel = "spring")
public interface ClassRoomMapper {

    /**
     * نگاشت درس.
     *
     * @param subject موجودیت درس
     * @return نمای درس
     */
    SubjectResponse toResponse(Subject subject);

    /**
     * نگاشت کلاس بدون نام درس.
     *
     * <p>⚠️ {@code subjectName} عمداً نادیده گرفته شده تا MapStruct
     * به‌جای حدس‌زدن، خطا ندهد. متد پیش‌فرض پایین‌تر آن را پر می‌کند.
     *
     * @param classRoom موجودیت کلاس
     * @return نمای کلاس با {@code subjectName} برابر {@code null}
     */
    @Mapping(target = "subjectName", ignore = true)
    ClassRoomResponse toResponse(ClassRoom classRoom);

    /**
     * نگاشت کلاس همراه نام درس.
     *
     * @param classRoom   موجودیت کلاس
     * @param subjectName نام درس، یا {@code null} اگر کلاس درسی ندارد
     * @return نمای کلاس
     */
    default ClassRoomResponse toResponse(ClassRoom classRoom, String subjectName) {
        ClassRoomResponse base = toResponse(classRoom);
        return new ClassRoomResponse(
                base.id(),
                base.name(),
                base.gradeLevel(),
                base.academicYear(),
                base.subjectId(),
                subjectName,
                base.homeroomTeacherId(),
                base.capacity(),
                base.status(),
                base.createdAt(),
                base.updatedAt());
    }
}
