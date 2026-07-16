package top.sharpcaterpillar.campusecho.message.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.message.domain.SiteMessage;
import top.sharpcaterpillar.campusecho.message.domain.SiteMessageRepository;

@Service
public class NotificationService {

    private final SiteMessageRepository siteMessageRepository;

    public NotificationService(SiteMessageRepository siteMessageRepository) {
        this.siteMessageRepository = siteMessageRepository;
    }

    @Transactional
    public SiteMessage notify(
            Long recipientId,
            Long senderId,
            Long tradeRequestId,
            Long reportId,
            Long itemId,
            MessageType messageType,
            String content) {
        return notify(recipientId, senderId, tradeRequestId, reportId, null, itemId, messageType, content);
    }

    @Transactional
    public SiteMessage notify(
            Long recipientId,
            Long senderId,
            Long tradeRequestId,
            Long reportId,
            Long disputeId,
            Long itemId,
            MessageType messageType,
            String content) {
        if (recipientId == null) {
            throw new IllegalArgumentException("recipientId is required");
        }
        if (senderId != null && senderId.equals(recipientId)) {
            return null;
        }
        return siteMessageRepository.save(SiteMessage.create(
                recipientId,
                senderId,
                tradeRequestId,
                reportId,
                disputeId,
                itemId,
                messageType,
                truncate(content)));
    }

    private String truncate(String content) {
        if (content == null || content.isBlank()) {
            return "系统通知";
        }
        String normalized = content.trim();
        return normalized.length() <= 2000 ? normalized : normalized.substring(0, 2000);
    }
}
