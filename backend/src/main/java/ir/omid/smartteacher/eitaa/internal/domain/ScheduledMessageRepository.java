package ir.omid.smartteacher.eitaa.internal.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, UUID> {

    @Query("""
            select m from ScheduledMessage m
            where m.status = 'PENDING'
              and m.scheduledAt <= :now
            order by m.scheduledAt asc
            """)
    List<ScheduledMessage> findDuePending(@Param("now") Instant now);
}
