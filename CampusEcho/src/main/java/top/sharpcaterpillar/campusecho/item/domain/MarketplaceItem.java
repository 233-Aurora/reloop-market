package top.sharpcaterpillar.campusecho.item.domain;

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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "items")
public class MarketplaceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 16)
    private ItemModerationStatus moderationStatus;

    @Column(name = "moderation_reason", length = 1000)
    private String moderationReason;

    @Column(name = "moderated_by")
    private Long moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "location_text", length = 255)
    private String locationText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MarketplaceItem() {
    }

    private MarketplaceItem(
            Long sellerId,
            Long categoryId,
            String title,
            String description,
            BigDecimal price,
            ItemCondition condition,
            String locationText) {
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.condition = condition;
        this.locationText = locationText;
        this.status = ItemStatus.ON_SALE;
        this.moderationStatus = ItemModerationStatus.NORMAL;
    }

    public static MarketplaceItem publish(
            Long sellerId,
            Long categoryId,
            String title,
            String description,
            BigDecimal price,
            ItemCondition condition,
            String locationText) {
        return new MarketplaceItem(sellerId, categoryId, title, description, price, condition, locationText);
    }

    public void update(
            Long categoryId,
            String title,
            String description,
            BigDecimal price,
            ItemCondition condition,
            String locationText) {
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.condition = condition;
        this.locationText = locationText;
    }

    public void changeStatus(ItemStatus status) {
        this.status = status;
    }

    public void block(String reason, Long administratorId, Instant moderatedAt) {
        this.moderationStatus = ItemModerationStatus.BLOCKED;
        this.moderationReason = reason;
        this.moderatedBy = administratorId;
        this.moderatedAt = moderatedAt;
    }

    public void restoreModeration(String reason, Long administratorId, Instant moderatedAt) {
        this.moderationStatus = ItemModerationStatus.NORMAL;
        this.moderationReason = reason;
        this.moderatedBy = administratorId;
        this.moderatedAt = moderatedAt;
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

    public Long getSellerId() {
        return sellerId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public ItemModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public String getLocationText() {
        return locationText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
