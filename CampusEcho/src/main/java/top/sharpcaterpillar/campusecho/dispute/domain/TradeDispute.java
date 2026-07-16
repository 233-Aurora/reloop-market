package top.sharpcaterpillar.campusecho.dispute.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "trade_disputes")
public class TradeDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_request_id", nullable = false)
    private Long tradeRequestId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "opener_id", nullable = false)
    private Long openerId;

    @Column(name = "respondent_id", nullable = false)
    private Long respondentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DisputeReason reason;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DisputeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private DisputeResolution resolution;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private Instant handledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TradeDispute() {
    }

    private TradeDispute(
            Long tradeRequestId,
            Long itemId,
            Long openerId,
            Long respondentId,
            DisputeReason reason,
            String description) {
        this.tradeRequestId = tradeRequestId;
        this.itemId = itemId;
        this.openerId = openerId;
        this.respondentId = respondentId;
        this.reason = reason;
        this.description = description;
        this.status = DisputeStatus.OPEN;
    }

    public static TradeDispute open(
            Long tradeRequestId,
            Long itemId,
            Long openerId,
            Long respondentId,
            DisputeReason reason,
            String description) {
        return new TradeDispute(tradeRequestId, itemId, openerId, respondentId, reason, description);
    }

    public void resolve(DisputeResolution resolution, String resolutionNote, Long handledBy, Instant handledAt) {
        this.status = DisputeStatus.RESOLVED;
        this.resolution = resolution;
        this.resolutionNote = resolutionNote;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
    }

    public void reject(String resolutionNote, Long handledBy, Instant handledAt) {
        this.status = DisputeStatus.REJECTED;
        this.resolution = DisputeResolution.NONE;
        this.resolutionNote = resolutionNote;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTradeRequestId() {
        return tradeRequestId;
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getOpenerId() {
        return openerId;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public DisputeReason getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public DisputeResolution getResolution() {
        return resolution;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public Instant getHandledAt() {
        return handledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
