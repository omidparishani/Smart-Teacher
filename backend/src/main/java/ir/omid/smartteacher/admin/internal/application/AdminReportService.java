package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.AdminChartsResponse;
import ir.omid.smartteacher.admin.internal.api.dto.AdminChartsResponse.DailyCount;
import ir.omid.smartteacher.admin.internal.api.dto.AiUsageReportResponse;
import ir.omid.smartteacher.admin.internal.api.dto.RevenueReportResponse;
import ir.omid.smartteacher.admin.internal.api.dto.SubscriptionsReportResponse;
import ir.omid.smartteacher.admin.internal.api.dto.UsersReportResponse;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries.ActivePlanSplit;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries.AiUsageAggregate;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries.DailyBucket;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries.DailyRevenueBucket;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries.NamedCostBucket;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries.RevenueAggregate;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries.UserAiUsageBucket;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * گزارش‌ها و نمودارهای پلتفرم‌سطح — با کش TTL سبک برای سری‌های سنگین.
 */
@Service
public class AdminReportService {

    private static final Pattern PERIOD_PATTERN =
            Pattern.compile("^(\\d+)([dD])$");

    private static final int MAX_PERIOD_DAYS = 366;

    /** TTL کش گزارش سنگین (نمودارها) — ۳۰ ثانیه. */
    private static final long CHARTS_CACHE_TTL_MS = 30_000L;

    private final PlatformStatsQueries platformStatsQueries;
    private final ConcurrentHashMap<String, CacheEntry<AdminChartsResponse>> chartsCache =
            new ConcurrentHashMap<>();

    /** شمارنده hit کش — فقط برای تست/مشاهده. */
    private final java.util.concurrent.atomic.AtomicLong chartsCacheHits =
            new java.util.concurrent.atomic.AtomicLong();

    public AdminReportService(PlatformStatsQueries platformStatsQueries) {
        this.platformStatsQueries = platformStatsQueries;
    }

    @Transactional(readOnly = true)
    public AdminChartsResponse charts(String period) {
        String key = "charts:" + (period == null ? "30d" : period.trim());
        Instant now = Instant.now();
        CacheEntry<AdminChartsResponse> cached = chartsCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            chartsCacheHits.incrementAndGet();
            return cached.value();
        }

        int days = parsePeriodDays(period);
        Instant toExclusive = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant fromInclusive = PlatformStatsQueries.startOfUtcDay(
                toExclusive.minus(days, ChronoUnit.DAYS));

        AdminChartsResponse response = new AdminChartsResponse(
                days + "d",
                fillGaps(platformStatsQueries.dailyNewUsers(fromInclusive, toExclusive),
                        fromInclusive, toExclusive),
                fillGaps(platformStatsQueries.dailyNewTenants(fromInclusive, toExclusive),
                        fromInclusive, toExclusive),
                fillGaps(platformStatsQueries.dailyNewSubscriptions(fromInclusive, toExclusive),
                        fromInclusive, toExclusive),
                fillRevenueGaps(platformStatsQueries.dailyRevenue(fromInclusive, toExclusive),
                        fromInclusive, toExclusive));

        chartsCache.put(key, new CacheEntry<>(response, now.plusMillis(CHARTS_CACHE_TTL_MS)));
        return response;
    }

    /** فقط برای تست. */
    public long chartsCacheHitCount() {
        return chartsCacheHits.get();
    }

    /** فقط برای تست — پاک‌سازی کش. */
    public void clearChartsCache() {
        chartsCache.clear();
        chartsCacheHits.set(0);
    }

    @Transactional(readOnly = true)
    public UsersReportResponse usersReport() {
        Map<String, Long> byStatus = platformStatsQueries.countUsersByStatus();
        return new UsersReportResponse(platformStatsQueries.countUsersTotal(), byStatus);
    }

    @Transactional(readOnly = true)
    public SubscriptionsReportResponse subscriptionsReport() {
        Map<String, Long> byStatus = platformStatsQueries.countSubscriptionsByStatus();
        long total = platformStatsQueries.countSubscriptionsTotal();
        long cancelled = byStatus.getOrDefault("CANCELLED", 0L);
        long expired = byStatus.getOrDefault("EXPIRED", 0L);
        double churn = total == 0 ? 0.0 : (double) (cancelled + expired) / (double) total;

        ActivePlanSplit split = platformStatsQueries.countActiveFreeVsPaid();
        long denom = split.freeActive() + split.paidActive();
        double conversion = denom == 0 ? 0.0
                : (double) split.paidActive() / (double) denom;

        return new SubscriptionsReportResponse(
                total, byStatus, churn, conversion, split.paidActive(), split.freeActive());
    }

    @Transactional(readOnly = true)
    public AiUsageReportResponse aiReport() {
        AiUsageAggregate agg = platformStatsQueries.sumAiUsage();
        List<AiUsageReportResponse.ByModel> byModel = platformStatsQueries.aiCostByModel().stream()
                .map(b -> new AiUsageReportResponse.ByModel(b.name(), b.requestCount(), b.totalCost()))
                .toList();
        List<AiUsageReportResponse.ByProvider> byProvider = platformStatsQueries.aiCostByProvider().stream()
                .map(b -> new AiUsageReportResponse.ByProvider(b.name(), b.requestCount(), b.totalCost()))
                .toList();
        List<AiUsageReportResponse.ByUser> byUser = platformStatsQueries.aiUsageByUser(20).stream()
                .map(AdminReportService::toByUser)
                .toList();
        return new AiUsageReportResponse(
                agg.requestCount(),
                agg.promptTokens(),
                agg.completionTokens(),
                agg.totalCost(),
                byModel,
                byProvider,
                byUser);
    }

    private static AiUsageReportResponse.ByUser toByUser(UserAiUsageBucket bucket) {
        return new AiUsageReportResponse.ByUser(
                bucket.userId(),
                bucket.userName(),
                bucket.requestCount(),
                bucket.promptTokens(),
                bucket.completionTokens(),
                bucket.totalCost());
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse revenueReport() {
        RevenueAggregate agg = platformStatsQueries.sumRevenue();
        return new RevenueReportResponse(
                agg.paidInvoiceCount(),
                agg.paidTotal(),
                agg.currency(),
                agg.byStatus());
    }

    @Transactional(readOnly = true)
    public String usersReportCsv() {
        UsersReportResponse report = usersReport();
        StringBuilder csv = new StringBuilder();
        csv.append("status,count\n");
        csv.append("TOTAL,").append(report.total()).append('\n');
        for (Map.Entry<String, Long> entry : report.byStatus().entrySet()) {
            csv.append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
        }
        return csv.toString();
    }

    /** خروجی Excel گزارش کاربران. */
    @Transactional(readOnly = true)
    public byte[] usersReportExcel() {
        UsersReportResponse report = usersReport();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("users");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("status");
            header.createCell(1).setCellValue("count");

            int rowIdx = 1;
            Row total = sheet.createRow(rowIdx++);
            total.createCell(0).setCellValue("TOTAL");
            total.createCell(1).setCellValue(report.total());

            for (Map.Entry<String, Long> entry : report.byStatus().entrySet()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "ساخت فایل Excel ناموفق بود.");
        }
    }

    private static int parsePeriodDays(String period) {
        if (period == null || period.isBlank()) {
            return 30;
        }
        Matcher matcher = PERIOD_PATTERN.matcher(period.trim());
        if (!matcher.matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "period باید به شکل Nd باشد (مثل 7d یا 30d).");
        }
        int days = Integer.parseInt(matcher.group(1));
        if (days < 1 || days > MAX_PERIOD_DAYS) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "طول period باید بین ۱ و " + MAX_PERIOD_DAYS + " روز باشد.");
        }
        return days;
    }

    private static List<AdminChartsResponse.DailyRevenue> fillRevenueGaps(
            List<DailyRevenueBucket> buckets,
            Instant fromInclusive,
            Instant toExclusive) {
        Map<LocalDate, DailyRevenueBucket> byDay = new LinkedHashMap<>();
        for (DailyRevenueBucket bucket : buckets) {
            byDay.put(bucket.day(), bucket);
        }

        LocalDate start = fromInclusive.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endInclusive = toExclusive.atZone(ZoneOffset.UTC).toLocalDate();

        List<AdminChartsResponse.DailyRevenue> series = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(endInclusive); day = day.plusDays(1)) {
            DailyRevenueBucket bucket = byDay.get(day);
            if (bucket == null) {
                series.add(new AdminChartsResponse.DailyRevenue(day.toString(), 0L, java.math.BigDecimal.ZERO));
            } else {
                series.add(new AdminChartsResponse.DailyRevenue(
                        day.toString(), bucket.invoiceCount(), bucket.revenue()));
            }
        }
        return series;
    }

    private static List<DailyCount> fillGaps(List<DailyBucket> buckets,
                                             Instant fromInclusive,
                                             Instant toExclusive) {
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (DailyBucket bucket : buckets) {
            byDay.put(bucket.day(), bucket.count());
        }

        LocalDate start = fromInclusive.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endInclusive = toExclusive.atZone(ZoneOffset.UTC).toLocalDate();

        List<DailyCount> series = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(endInclusive); day = day.plusDays(1)) {
            series.add(new DailyCount(day.toString(), byDay.getOrDefault(day, 0L)));
        }
        return series;
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
