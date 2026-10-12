package ir.omid.smartteacher.eitaa.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelPublishQueueRepository extends JpaRepository<ChannelPublishQueueItem, UUID> {

    List<ChannelPublishQueueItem> findByStatusOrderByCreatedAtAsc(String status);
}
