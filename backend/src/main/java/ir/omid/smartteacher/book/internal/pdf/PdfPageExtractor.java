package ir.omid.smartteacher.book.internal.pdf;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * استخراج متن صفحات PDF با PDFBox — بدون رندر تصویر.
 *
 * <p>هر صفحه به یک placeholder متنی تبدیل می‌شود تا در تست با PDF
 * کوچک بدون فونت/رندر کار کند.
 */
@Component
public class PdfPageExtractor {

    /**
     * استخراج متن هر صفحه.
     *
     * @param pdfContent جریان PDF
     * @return فهرست متن صفحات (ایندکس ۰ = صفحه ۱)
     */
    public List<String> extractPageTexts(InputStream pdfContent) {
        if (pdfContent == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "فایل PDF خالی است.");
        }
        try (PDDocument document = Loader.loadPDF(pdfContent.readAllBytes())) {
            int pages = document.getNumberOfPages();
            if (pages < 1) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "PDF هیچ صفحه‌ای ندارد.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> texts = new ArrayList<>(pages);
            for (int i = 1; i <= pages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                if (text == null || text.isBlank()) {
                    text = "[صفحه " + i + " — بدون متن قابل استخراج]";
                }
                texts.add(text.trim());
            }
            return texts;
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "خواندن PDF ناموفق بود یا فایل خراب است.", null, ex);
        }
    }
}
