package top.sharpcaterpillar.campusecho.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "item_images")
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ItemImage() {
    }

    private ItemImage(Long itemId, String storageKey, int sortOrder, boolean primary) {
        this.itemId = itemId;
        this.storageKey = storageKey;
        this.sortOrder = sortOrder;
        this.primary = primary;
    }

    public static ItemImage create(Long itemId, String storageKey, int sortOrder, boolean primary) {
        return new ItemImage(itemId, storageKey, sortOrder, primary);
    }

    public void makePrimary() {
        this.primary = true;
    }

    public void removePrimary() {
        this.primary = false;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
