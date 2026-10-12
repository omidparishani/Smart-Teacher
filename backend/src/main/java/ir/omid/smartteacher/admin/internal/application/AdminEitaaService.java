package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.AdminEitaaOverviewResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * نمای کلی ایتا در سطح پلتفرم — JDBC فرامستأجری.
 */
@Service
public class AdminEitaaService {

    private final JdbcTemplate jdbc;

    public AdminEitaaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public AdminEitaaOverviewResponse overview() {
        Long groups = jdbc.queryForObject(
                "select count(*) from eitaa_groups where deleted_at is null",
                Long.class);
        Long channels = jdbc.queryForObject(
                "select count(*) from eitaa_channels where deleted_at is null",
                Long.class);
        Long tracked = jdbc.queryForObject(
                "select count(*) from message_credits",
                Long.class);
        Long balance = jdbc.queryForObject(
                "select coalesce(sum(balance), 0) from message_credits",
                Long.class);
        Long quota = jdbc.queryForObject(
                "select coalesce(sum(free_quota), 0) from message_credits",
                Long.class);

        return new AdminEitaaOverviewResponse(
                nullToZero(groups),
                nullToZero(channels),
                nullToZero(tracked),
                nullToZero(balance),
                nullToZero(quota));
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
