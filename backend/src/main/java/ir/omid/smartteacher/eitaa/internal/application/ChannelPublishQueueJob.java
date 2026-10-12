package ir.omid.smartteacher.eitaa.internal.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * جاب stub صف انتشار کانال — فعلاً فقط لاگ؛ ارسال واقعی در گام بعد.
 */
@Component
@ConditionalOnProperty(name = "app.eitaa.channel-queue.enabled", havingValue = "true")
public class ChannelPublishQueueJob {

    private static final Logger log = LoggerFactory.getLogger(ChannelPublishQueueJob.class);

    private final ChannelPublishQueueService queueService;

    public ChannelPublishQueueJob(ChannelPublishQueueService queueService) {
        this.queueService = queueService;
    }

    @Scheduled(fixedDelayString = "${app.eitaa.channel-queue.fixed-delay-ms:60000}")
    public void processPending() {
        int pending = queueService.listPending().size();
        if (pending > 0) {
            log.info("صف انتشار کانال: {} آیتم PENDING (stub — ارسال در گام بعد)", pending);
        }
    }
}
