package ir.omid.smartteacher.admin.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

    Optional<SystemSetting> findByKey(String key);

    List<SystemSetting> findAllByOrderByCategoryAscKeyAsc();
}
