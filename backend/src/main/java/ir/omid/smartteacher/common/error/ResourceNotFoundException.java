package ir.omid.smartteacher.common.error;

/**
 * منبع درخواستی وجود ندارد — یا کاربر اجازه دیدن آن را ندارد.
 *
 * <p><b>استراتژی عمدی:</b> این استثنا هم برای «وجود ندارد» و هم برای
 * «مال مستأجر دیگری است» استفاده می‌شود. هیچ‌گاه نباید برای مورد دوم
 * {@code 403} برگرداند.
 *
 * <p><b>چرا؟</b> اگر منبع موجودِ متعلق به دیگری {@code 403} بدهد و منبع
 * ناموجود {@code 404}، مهاجم با شمردن کدهای وضعیت می‌فهمد کدام شناسه
 * وجود دارد و کدام نه — و همین، فهرست شناسه‌های معتبر را برایش می‌سازد.
 * یکسان‌بودن پاسخ، این کانال جانبی را می‌بندد. این قاعده در
 * {@code docs/security.md} و {@code docs/multi-tenancy.md} الزامی شده است.
 */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * پیام عمومی — برای همه جاهایی که ممکن است کنترل دسترسی دخیل باشد.
     * در جست‌وجوهای تحت محافظت ({@code TenantGuard}) <b>فقط</b> از این
     * استفاده کنید.
     */
    public static ResourceNotFoundException generic() {
        return new ResourceNotFoundException();
    }

    /**
     * پیام اختصاصی — فقط برای منبعی که کاربر قطعاً به آن دسترسی دارد و
     * صرفاً وجود ندارد (مثلاً پروفایل خودش).
     *
     * @param resourceName نام فارسی منبع، مثل «کلاس» یا «آزمون»
     */
    public static ResourceNotFoundException of(String resourceName) {
        return new ResourceNotFoundException(resourceName);
    }

    private ResourceNotFoundException(String resourceName) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "«" + resourceName + "» مورد نظر یافت نشد.");
    }
}
