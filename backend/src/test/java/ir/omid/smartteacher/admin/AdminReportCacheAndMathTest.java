package ir.omid.smartteacher.admin;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.admin.internal.application.AdminReportService;
import ir.omid.smartteacher.admin.internal.api.dto.AdminChartsResponse;
import ir.omid.smartteacher.admin.internal.api.dto.SubscriptionsReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * ریاضی گزارش churn/conversion + cache hit.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AdminReportCacheAndMathTest {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    @Autowired
    private AdminReportService reportService;

    @Test
    @DisplayName("churn و conversion تقریب‌های غیرمنفی هستند")
    void reportMath_churnAndConversion() {
        SubscriptionsReportResponse report = reportService.subscriptionsReport();
        assertThat(report.churnRateApprox()).isBetween(0.0, 1.0);
        assertThat(report.conversionRateApprox()).isBetween(0.0, 1.0);
        assertThat(report.paidActiveCount() + report.freeActiveCount())
                .isLessThanOrEqualTo(report.total());
    }

    @Test
    @DisplayName("دومین فراخوانی charts از کش hit می‌شود")
    void chartsCache_hit() {
        reportService.clearChartsCache();
        AdminChartsResponse first = reportService.charts("7d");
        assertThat(first.period()).isEqualTo("7d");
        assertThat(reportService.chartsCacheHitCount()).isZero();

        AdminChartsResponse second = reportService.charts("7d");
        assertThat(second.period()).isEqualTo(first.period());
        assertThat(reportService.chartsCacheHitCount()).isEqualTo(1);
    }
}
