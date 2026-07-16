package top.sharpcaterpillar.campusecho.message.api;

import java.time.Instant;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.message.domain.SiteMessage;

public record MessageResponse(
        Long id,
        MessageType messageType,
        String content,
        boolean read,
        Long senderId,
        String senderUsername,
        Long tradeRequestId,
        Long reportId,
        Long disputeId,
        Long itemId,
        Instant createdAt) {

    public static MessageResponse from(SiteMessage message, String senderUsername) {
        return new MessageResponse(
                message.getId(),
                message.getMessageType(),
                message.getContent(),
                message.isRead(),
                message.getSenderId(),
                senderUsername,
                message.getTradeRequestId(),
                message.getReportId(),
                message.getDisputeId(),
                message.getItemId(),
                message.getCreatedAt());
    }
}
