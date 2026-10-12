package ir.omid.smartteacher.student.internal.api.mapper;

import ir.omid.smartteacher.student.internal.api.dto.StudentResponse;
import ir.omid.smartteacher.student.internal.domain.Student;
import org.mapstruct.Mapper;

/**
 * نگاشت موجودیت دانش‌آموز به DTO وب.
 *
 * <p>پیاده‌سازی ({@code StudentMapperImpl}) در زمان کامپایل ساخته
 * می‌شود. هیچ فیلد دستی نگاشته نمی‌شود تا افزودن یک فیلد تازه به موجودیت
 * بی‌صدا از قلم نیفتد — اگر فیلدی نگاشته نشود، MapStruct هشدار می‌دهد.
 */
@Mapper(componentModel = "spring")
public interface StudentMapper {

    /**
     * نگاشت دانش‌آموز.
     *
     * @param student موجودیت دانش‌آموز
     * @return نمای دانش‌آموز
     */
    StudentResponse toResponse(Student student);
}
