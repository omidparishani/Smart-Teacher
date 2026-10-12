package ir.omid.smartteacher.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * قید پذیرش فاز ۴: هر متد تغییردهنده روی کنترلرهای {@code /admin/**}
 * باید {@code @PreAuthorize} داشته باشد — fail-closed.
 */
class ArchUnit_adminMutatingEndpointsRequirePreAuthorizeTest {

    private static final Set<String> MUTATING_MAPPING_NAMES = Set.of(
            PostMapping.class.getName(),
            PutMapping.class.getName(),
            PatchMapping.class.getName(),
            DeleteMapping.class.getName());

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ir.omid.smartteacher");
    }

    @Test
    @DisplayName("متدهای تغییردهنده کنترلرهای /admin/** باید @PreAuthorize داشته باشند")
    void mutatingAdminEndpointsMustHavePreAuthorize() {
        List<String> offenders = new ArrayList<>();

        for (JavaClass javaClass : productionClasses) {
            if (!javaClass.isAnnotatedWith(RestController.class)) {
                continue;
            }
            if (!isAdminController(javaClass)) {
                continue;
            }

            for (JavaMethod method : javaClass.getMethods()) {
                if (!isMutatingMapping(method)) {
                    continue;
                }
                if (!method.isAnnotatedWith(PreAuthorize.class)
                        && !javaClass.isAnnotatedWith(PreAuthorize.class)) {
                    offenders.add(javaClass.getSimpleName() + "#" + method.getName());
                }
            }
        }

        assertThat(offenders)
                .as("Endpoint تغییردهنده ادمین بدون @PreAuthorize — fail-closed نقض شده")
                .isEmpty();

        assertThat(productionClasses).isNotEmpty();
    }

    @Test
    @DisplayName("حداقل یک کنترلر ادمین با متد تغییردهنده پیدا می‌شود — آزمون بی‌معنا نیست")
    void scanFindsAdminMutatingEndpoints() {
        int mutating = 0;
        for (JavaClass javaClass : productionClasses) {
            if (!javaClass.isAnnotatedWith(RestController.class) || !isAdminController(javaClass)) {
                continue;
            }
            for (JavaMethod method : javaClass.getMethods()) {
                if (isMutatingMapping(method)) {
                    mutating++;
                }
            }
        }
        assertThat(mutating)
                .as("اگر صفر باشد، قاعده PreAuthorize چیزی را نمی‌سنجد")
                .isGreaterThan(0);
    }

    private static boolean isAdminController(JavaClass javaClass) {
        if (javaClass.isAnnotatedWith(RequestMapping.class)) {
            String classPath = mappingPath(javaClass.getAnnotationOfType(RequestMapping.class));
            if (classPath != null && classPath.toLowerCase(Locale.ROOT).contains("/admin")) {
                return true;
            }
        }
        for (JavaMethod method : javaClass.getMethods()) {
            for (JavaAnnotation<?> annotation : method.getAnnotations()) {
                if (!annotation.getRawType().getName().endsWith("Mapping")) {
                    continue;
                }
                Object value = annotation.getProperties().get("value");
                if (value instanceof Object[] array) {
                    for (Object element : array) {
                        if (String.valueOf(element).toLowerCase(Locale.ROOT).contains("/admin")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isMutatingMapping(JavaMethod method) {
        for (JavaAnnotation<?> annotation : method.getAnnotations()) {
            if (MUTATING_MAPPING_NAMES.contains(annotation.getRawType().getName())) {
                return true;
            }
        }
        return false;
    }

    private static String mappingPath(RequestMapping mapping) {
        if (mapping == null) {
            return null;
        }
        if (mapping.value().length > 0) {
            return mapping.value()[0];
        }
        if (mapping.path().length > 0) {
            return mapping.path()[0];
        }
        return null;
    }
}
