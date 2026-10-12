package ir.omid.smartteacher.student.internal.application;

import ir.omid.smartteacher.student.StudentDirectory;
import ir.omid.smartteacher.student.internal.domain.Student;
import ir.omid.smartteacher.student.internal.domain.StudentRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * پیاده‌سازی {@link StudentDirectory} — تنها درِ خروجی داده دانش‌آموز
 * از این ماژول.
 *
 * <p>⚠️ این کلاس هیچ منطق کسب‌وکاری ندارد و <b>نباید</b> داشته باشد:
 * فقط ترجمه است. هر تصمیمی اینجا یعنی قاعده‌ای که مصرف‌کننده‌ها
 * نمی‌بینند و نمی‌توانند تستش کنند.
 *
 * <p>⚠️ {@code @Transactional(readOnly = true)} لازم است، چون
 * مصرف‌کننده‌ها (مثل {@code classroom}) از داخل تراکنش خودشان صدایش
 * می‌زنند و این متد باید در همان تراکنش بنشیند — بدون آن، هر فراخوانی
 * یک Session تازه می‌گرفت.
 */
@Component
class StudentDirectoryImpl implements StudentDirectory {

    private final StudentRepository studentRepository;

    StudentDirectoryImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, StudentSummary> summariesOf(Collection<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            // ⚠️ بازگشت زودهنگام عمدی است: بدون آن، یک کوئری با
            // `in ()` خالی به دیتابیس می‌رفت که بعضی درایورها ردش
            // می‌کنند و بعضی بی‌صدا همه رکوردها را برمی‌گردانند.
            return Map.of();
        }

        return studentRepository.findByIdInAndDeletedAtIsNull(studentIds).stream()
                .collect(Collectors.toMap(
                        Student::getId,
                        student -> new StudentSummary(
                                student.getId(),
                                student.getFullName(),
                                student.getNationalCode())));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> activeIdsAmong(Collection<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Set.of();
        }

        // ⚠️ سه دلیل که اینجا مستقیم کوئری می‌زنیم و summariesOf را
        // صدا نمی‌زنیم: (۱) ساخت خلاصه‌ها اینجا کار بی‌فایده است،
        // (۲) Set تضمین یکتایی می‌دهد و (۳) اگر روزی این متد پرمصرف
        // شد، یک کوئری سبک‌تر جایگزینش می‌شود بدون تغییر امضا.
        return studentRepository.findByIdInAndDeletedAtIsNull(studentIds).stream()
                .map(Student::getId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
