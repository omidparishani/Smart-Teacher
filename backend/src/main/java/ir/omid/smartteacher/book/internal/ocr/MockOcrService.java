package ir.omid.smartteacher.book.internal.ocr;

import ir.omid.smartteacher.book.OcrService;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/**
 * OCR جایگزین — بدون فراخوانی ابر.
 *
 * <p>گام ۱ فاز ۶ فقط Interface را سیم‌کشی می‌کند تا بعداً پیاده‌سازی
 * واقعی جایگزین شود بدون تغییر سرویس کتاب. این کلاس شکل API هیچ
 * ارائه‌دهنده‌ای را جعل نمی‌کند.
 */
@Service
public class MockOcrService implements OcrService {

    @Override
    public OcrResult extractText(InputStream imageContent, String contentType) {
        // جریان عمداً خوانده نمی‌شود — برای Mock لازم نیست و مصرف حافظه
        // روی تصویر بزرگ را کم می‌کند.
        return OcrResult.placeholder();
    }
}
