package top.sharpcaterpillar.campusecho.item.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import top.sharpcaterpillar.campusecho.item.domain.ItemCondition;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;

public record ItemResponse(
        Long id,
        Long sellerId,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        ItemCondition condition,
        ItemStatus status,
        ItemModerationStatus moderationStatus,
        String moderationReason,
        String locationText,
        Instant createdAt,
        Instant updatedAt,
        List<ItemImageResponse> images) {

    public static ItemResponse from(MarketplaceItem item, String categoryName) {
        return from(item, categoryName, List.of());
    }

    public static ItemResponse from(MarketplaceItem item, String categoryName, List<ItemImageResponse> images) {
        return new ItemResponse(
                item.getId(),
                item.getSellerId(),
                item.getCategoryId(),
                categoryName,
                item.getTitle(),
                item.getDescription(),
                item.getPrice(),
                item.getCondition(),
                item.getStatus(),
                item.getModerationStatus(),
                item.getModerationStatus() == ItemModerationStatus.BLOCKED ? item.getModerationReason() : null,
                item.getLocationText(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                images);
    }
}
