package top.sharpcaterpillar.campusecho.trade.domain;

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
@Table(name = "trade_requests")
public class TradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TradeStatus status;

    @Column(name = "requester_confirmed_at")
    private Instant requesterConfirmedAt;

    @Column(name = "seller_confirmed_at")
    private Instant sellerConfirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TradeRequest() {
    }

    private TradeRequest(Long itemId, Long requesterId, Long sellerId, String message) {
        this.itemId = itemId;
        this.requesterId = requesterId;
        this.sellerId = sellerId;
        this.message = message;
        this.status = TradeStatus.PENDING;
    }

    public static TradeRequest create(Long itemId, Long requesterId, Long sellerId, String message) {
        return new TradeRequest(itemId, requesterId, sellerId, message);
    }

    public void accept() {
        status = TradeStatus.ACCEPTED;
    }

    public void reject() {
        status = TradeStatus.REJECTED;
    }

    public void cancel() {
        status = TradeStatus.CANCELLED;
    }

    public void confirmBy(Long userId, Instant confirmedAt) {
        if (requesterId.equals(userId)) {
            requesterConfirmedAt = confirmedAt;
        } else if (sellerId.equals(userId)) {
            sellerConfirmedAt = confirmedAt;
        }
    }

    public void complete() {
        status = TradeStatus.COMPLETED;
    }

    public void markDisputed() {
        status = TradeStatus.DISPUTED;
    }

    public void completeFromDispute(Instant confirmedAt) {
        if (requesterConfirmedAt == null) {
            requesterConfirmedAt = confirmedAt;
        }
        if (sellerConfirmedAt == null) {
            sellerConfirmedAt = confirmedAt;
        }
        status = TradeStatus.COMPLETED;
    }

    public void cancelFromDispute() {
        status = TradeStatus.CANCELLED;
    }

    public void reopenAfterDisputeRejected() {
        status = TradeStatus.ACCEPTED;
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

    public Long getItemId() {
        return itemId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getMessage() {
        return message;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public Instant getRequesterConfirmedAt() {
        return requesterConfirmedAt;
    }

    public Instant getSellerConfirmedAt() {
        return sellerConfirmedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
