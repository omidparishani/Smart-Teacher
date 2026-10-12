package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.AdminDashboardResponse;
import ir.omid.smartteacher.admin.internal.api.dto.AdminDashboardResponse.EducationStats;
import ir.omid.smartteacher.admin.internal.api.dto.AdminDashboardResponse.TenantStats;
import ir.omid.smartteacher.admin.internal.api.dto.AdminDashboardResponse.UserStats;
import ir.omid.smartteacher.admin.internal.platform.PlatformStatsQueries;
import ir.omid.smartteacher.tenant.TenantRepository;
import ir.omid.smartteacher.user.UserRepository;
import ir.omid.smartteacher.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * داشبورد پایه ادمین — شمارنده‌های واقعی از جدول‌های موجود.
 *
 * <p>کاربران و مستأجرها سطح پلتفرم‌اند و از Repository خودشان خوانده
 * می‌شوند. کلاس و دانش‌آموز فیلتر مستأجر دارند؛ شمارش فرامستأجری‌شان
 * از {@link PlatformStatsQueries} می‌آید.
 */
@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PlatformStatsQueries platformStatsQueries;

    public AdminDashboardService(UserRepository userRepository,
                                 TenantRepository tenantRepository,
                                 PlatformStatsQueries platformStatsQueries) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.platformStatsQueries = platformStatsQueries;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        long usersTotal = userRepository.countByDeletedAtIsNull();
        long usersActive = userRepository.countByStatusAndDeletedAtIsNull(UserStatus.ACTIVE);
        long tenantsTotal = tenantRepository.countByDeletedAtIsNull();
        long classrooms = platformStatsQueries.countClassrooms();
        long students = platformStatsQueries.countStudents();

        return new AdminDashboardResponse(
                new UserStats(usersTotal, usersActive),
                new TenantStats(tenantsTotal),
                new EducationStats(classrooms, students));
    }
}
