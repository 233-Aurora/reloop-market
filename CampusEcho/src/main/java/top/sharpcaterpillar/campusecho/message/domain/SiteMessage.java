package top.sharpcaterpillar.campusecho.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class SiteMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "trade_request_id")
    private Long tradeRequestId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "dispute_id")
    private Long disputeId;

    @Column(name = "item_id")
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    private MessageType messageType;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SiteMessage() {
    }

    private SiteMessage(
            Long recipientId,
            Long senderId,
            Long tradeRequestId,
            Long reportId,
            Long disputeId,
            Long itemId,
            MessageType messageType,
            String content) {
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.tradeRequestId = tradeRequestId;
        this.reportId = reportId;
        this.disputeId = disputeId;
        this.itemId = itemId;
        this.messageType = messageType;
        this.content = content;
        this.read = false;
    }

    public static SiteMessage create(
            Long recipientId,
            Long senderId,
            Long tradeRequestId,
            Long reportId,
            Long disputeId,
            Long itemId,
            MessageType messageType,
            String content) {
        return new SiteMessage(
                recipientId, senderId, tradeRequestId, reportId, disputeId, itemId, messageType, content);
    }

    public void markRead() {
        this.read = true;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getTradeRequestId() {
        return tradeRequestId;
    }

    public Long getReportId() {
        return reportId;
    }

    public Long getDisputeId() {
        return disputeId;
    }

    public Long getItemId() {
        return itemId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
