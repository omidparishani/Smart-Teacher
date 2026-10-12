package ir.omid.smartteacher.eitaa.internal.application;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.internal.MockAiProvider;
import ir.omid.smartteacher.eitaa.internal.api.dto.ChannelPublishQueueResponse;
import ir.omid.smartteacher.eitaa.internal.domain.ChannelPublishQueueItem;
import ir.omid.smartteacher.eitaa.internal.domain.ChannelPublishQueueRepository;
import ir.omid.smartteacher.eitaa.internal.domain.EitaaChannel;
import ir.omid.smartteacher.eitaa.internal.domain.EitaaChannelRepository;
import ir.omid.smartteacher.security.TenantGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * صف انتشار کانال — Mock-AI متن می‌سازد و PENDING می‌ماند.
 */
@Service
public class ChannelPublishQueueService {

    private final ChannelPublishQueueRepository queueRepository;
    private final EitaaChannelRepository channelRepository;
    private final ObjectProvider<AiProvider> aiProvider;
    private final TenantGuard tenantGuard;

    public ChannelPublishQueueService(ChannelPublishQueueRepository queueRepository,
                                      EitaaChannelRepository channelRepository,
                                      ObjectProvider<AiProvider> aiProvider,
                                      TenantGuard tenantGuard) {
        this.queueRepository = queueRepository;
        this.channelRepository = channelRepository;
        this.aiProvider = aiProvider;
        this.tenantGuard = tenantGuard;
    }

    @Transactional(readOnly = true)
    public List<ChannelPublishQueueResponse> listPending() {
        return queueRepository.findByStatusOrderByCreatedAtAsc("PENDING").stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChannelPublishQueueResponse enqueue(UUID channelId, String topic) {
        requireChannel(channelId);
        String content = generateContent(topic);
        ChannelPublishQueueItem item = queueRepository.save(
                new ChannelPublishQueueItem(channelId, topic.trim(), content));
        return toResponse(item);
    }

    private String generateContent(String topic) {
        AiProvider provider = aiProvider.getIfAvailable();
        if (provider == null) {
            return "📢 " + topic.trim();
        }
        AiProvider.AiResponse response = provider.complete(AiProvider.AiRequest.ofUserPrompt(
                MockAiProvider.DEFAULT_MODEL,
                "یک پست کوتاه فارسی برای کانال آموزشی با موضوع: " + topic.trim()));
        String text = response.content();
        return text == null || text.isBlank() ? "📢 " + topic.trim() : text.trim();
    }

    private EitaaChannel requireChannel(UUID id) {
        EitaaChannel channel = channelRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        return tenantGuard.requireOwned(channel, "EitaaChannel");
    }

    private ChannelPublishQueueResponse toResponse(ChannelPublishQueueItem item) {
        return new ChannelPublishQueueResponse(
                item.getId(),
                item.getChannelId(),
                item.getTopic(),
                item.getContentText(),
                item.getStatus(),
                item.getCreatedAt());
    }
}
