package top.sharpcaterpillar.campusecho.report.api;

import java.time.Instant;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;

public record ItemModerationResponse(
        Long itemId,
        ItemStatus status,
        ItemModerationStatus moderationStatus,
        String moderationReason,
        Long moderatedBy,
        Instant moderatedAt) {

    public static ItemModerationResponse from(MarketplaceItem item) {
        return new ItemModerationResponse(
                item.getId(),
                item.getStatus(),
                item.getModerationStatus(),
                item.getModerationReason(),
                item.getModeratedBy(),
                item.getModeratedAt());
    }
}
