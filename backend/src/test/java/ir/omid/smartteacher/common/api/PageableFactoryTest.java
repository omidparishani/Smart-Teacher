package ir.omid.smartteacher.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * آزمون سقف و فهرست مجاز صفحه‌بندی.
 *
 * <p>این کلاس مرز بین «درخواست کاربر» و «کوئری دیتابیس» است، پس هر
 * ورودی نامعتبری که از اینجا رد شود، در دیتابیس هزینه واقعی می‌سازد.
 * هر تست اینجا یک راه نفوذ بسته‌شده را قفل می‌کند.
 */
class PageableFactoryTest {

    private static final Set<String> ALLOWED =
            Set.of("name", "createdAt", "updatedAt");

    private static final String DEFAULT_SORT = "createdAt,desc";

    private static Pageable build(int page, int size, String sort) {
        return PageableFactory.of(page, size, sort, ALLOWED, DEFAULT_SORT);
    }

    // ─────────────────────────── اندازه ───────────────────────────

    @Test
    @DisplayName("اندازه بزرگ‌تر از سقف، کلمپ می‌شود نه رد")
    void oversizedPageIsClamped() {
        // ⚠️ کلمپ و نه خطا: درخواست ۵۰۰۰تایی بی‌معنا نیست، فقط
        // بلندپروازانه است. رد کردنش کلاینت درست‌کار را هم می‌شکند.
        assertThat(build(0, 5000, null).getPageSize())
                .as("سقف باید اعمال شود")
                .isEqualTo(PageableFactory.MAX_SIZE);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("اندازه صفر یا منفی به پیش‌فرض برمی‌گردد")
    void nonPositiveSizeFallsBackToDefault(int size) {
        assertThat(build(0, size, null).getPageSize()).isEqualTo(PageableFactory.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("اندازه در محدوده دست‌نخورده می‌ماند")
    void sizeWithinBoundsIsKept() {
        assertThat(build(0, 37, null).getPageSize()).isEqualTo(37);
    }

    @Test
    @DisplayName("شماره صفحه منفی به صفر کلمپ می‌شود")
    void negativePageIsClampedToZero() {
        // ⚠️ بدون این، PageRequest.of استثنا می‌پراند و کاربر به‌جای
        // «صفحه اول»، خطای سرور می‌گیرد.
        assertThat(build(-5, 20, null).getPageNumber()).isZero();
    }

    // ─────────────────────────── مرتب‌سازی ───────────────────────────

    @Test
    @DisplayName("نبودِ مرتب‌سازی یعنی مرتب‌سازی پیش‌فرض")
    void missingSortFallsBackToDefault() {
        Pageable pageable = build(0, 20, null);

        assertThat(pageable.getSort().getOrderFor("createdAt"))
                .as("مرتب‌سازی پیش‌فرض باید اعمال شود")
                .isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("مرتب‌سازی بدون جهت، صعودی است")
    void sortWithoutDirectionIsAscending() {
        Pageable pageable = build(0, 20, "name");

        assertThat(pageable.getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("جهت نزولی خوانده می‌شود")
    void descendingIsParsed() {
        Pageable pageable = build(0, 20, "name,desc");

        assertThat(pageable.getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("فیلد خارج از فهرست مجاز رد می‌شود")
    void fieldOutsideAllowListIsRejected() {
        // 🔴 این تست مهم‌ترین تست این کلاس است. بدون این قید، مهاجم با
        // درخواست مرتب‌سازی روی هر ستونی که نامش را حدس بزند، برای
        // هر درخواست یک مرتب‌سازی و یک پیمایش کامل می‌سازد.
        assertThatThrownBy(() -> build(0, 20, "tenantId,asc"))
                .as("فیلد نامجاز باید صریحاً رد شود، نه بی‌صدا نادیده گرفته شود")
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("پیام فیلد نامجاز، نام فیلد را می‌گوید ولی فهرست مجاز را نه")
    void rejectionNamesTheFieldButNotTheAllowList() {
        // ⚠️ تفاوت مهم: نام فیلد ورودی خود کاربر است و گفتنش کمکی است؛
        // فهرست مجاز نقشه ستون‌های ماست و نباید لو برود.
        assertThatThrownBy(() -> build(0, 20, "secretColumn,asc"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("secretColumn")
                .hasMessageNotContaining("createdAt");
    }

    @Test
    @DisplayName("جهت نامعتبر رد می‌شود")
    void invalidDirectionIsRejected() {
        // ⚠️ Sort.Direction.fromString برای «ascending» هم استثنا
        // می‌دهد. اگر ما آن استثنا را نمی‌گرفتیم، کاربر به‌جای پیام
        // روشن «فقط asc و desc»، خطای سرور می‌گرفت.
        assertThatThrownBy(() -> build(0, 20, "name,sideways"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("فیلد مجاز با فاصله اضافه هم پذیرفته می‌شود")
    void surroundingWhitespaceInSortIsTolerated() {
        assertThat(build(0, 20, "  name , desc ").getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
