package ir.omid.smartteacher.security.rbac;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * بارگذاری کدهای مجوز ({@code permissions.code}) برای نقش‌های JWT.
 *
 * <p>فقط برای {@code GrantedAuthority} — نقش‌ها همچنان جداگانه اضافه می‌شوند.
 */
@Component
public class RolePermissionAuthorityLoader {

    private final JdbcTemplate jdbc;

    public RolePermissionAuthorityLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Set<String> permissionsForRoles(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(roleCodes.size(), "?"));
        String sql = """
                select distinct p.code
                  from roles r
                  join role_permissions rp on rp.role_id = r.id
                  join permissions p on p.id = rp.permission_id
                 where r.code in (%s)
                """.formatted(placeholders);
        List<String> codes = jdbc.queryForList(sql, String.class, roleCodes.toArray());
        return Collections.unmodifiableSet(new LinkedHashSet<>(codes));
    }
}
