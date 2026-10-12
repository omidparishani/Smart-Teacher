package ir.omid.smartteacher.admin.internal.api.dto;

/** نمای کلی ایتا برای پنل ادمین — فقط خواندنی. */
public record AdminEitaaOverviewResponse(
        long groupCount,
        long channelCount,
        long tenantCreditsTracked,
        long totalCreditsBalance,
        long totalCreditsQuota
) {
}
