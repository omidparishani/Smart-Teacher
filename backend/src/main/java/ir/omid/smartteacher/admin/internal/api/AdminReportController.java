package ir.omid.smartteacher.admin.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.admin.internal.api.dto.AdminChartsResponse;
import ir.omid.smartteacher.admin.internal.api.dto.AiUsageReportResponse;
import ir.omid.smartteacher.admin.internal.api.dto.RevenueReportResponse;
import ir.omid.smartteacher.admin.internal.api.dto.SubscriptionsReportResponse;
import ir.omid.smartteacher.admin.internal.api.dto.UsersReportResponse;
import ir.omid.smartteacher.admin.internal.application.AdminReportService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * گزارش‌ها و نمودارهای ادمین — فاز ۱۱ گام ۱.
 *
 * <p>فقط اعداد واقعی از جدول‌های موجود؛ دامنهٔ بدون منبع داده عمداً غایب است.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "گزارش ادمین", description = "نمودارها و گزارش‌های تجمیعی پلتفرم")
public class AdminReportController {

    static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final AdminReportService reportService;

    public AdminReportController(AdminReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard/charts")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "نمودارهای روزانه",
            description = "کاربران/مستأجرها/اشتراک‌های جدید روزانه برای period (مثل 30d).")
    public AdminChartsResponse charts(
            @RequestParam(name = "period", defaultValue = "30d") String period) {
        return reportService.charts(period);
    }

    @GetMapping("/reports/users")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "گزارش کاربران", description = "شمارش بر اساس وضعیت.")
    public UsersReportResponse usersReport() {
        return reportService.usersReport();
    }

    @GetMapping("/reports/subscriptions")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "گزارش اشتراک‌ها", description = "شمارش بر اساس وضعیت از subscriptions.")
    public SubscriptionsReportResponse subscriptionsReport() {
        return reportService.subscriptionsReport();
    }

    @GetMapping("/reports/ai")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "گزارش مصرف AI", description = "جمع توکن و هزینه از ai_usage_records.")
    public AiUsageReportResponse aiReport() {
        return reportService.aiReport();
    }

    @GetMapping("/reports/revenue")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "گزارش درآمد", description = "جمع فاکتورهای PAID از invoices.")
    public RevenueReportResponse revenueReport() {
        return reportService.revenueReport();
    }

    @GetMapping(value = "/reports/users.csv", produces = "text/csv")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "خروجی CSV گزارش کاربران")
    public ResponseEntity<byte[]> usersReportCsv() {
        String csv = reportService.usersReportCsv();
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"users-report.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping(value = "/reports/users.xlsx")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "خروجی Excel گزارش کاربران")
    public ResponseEntity<byte[]> usersReportExcel() {
        byte[] body = reportService.usersReportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"users-report.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
