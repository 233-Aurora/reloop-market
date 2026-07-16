package top.sharpcaterpillar.campusecho.item.api;

import java.time.Instant;
import top.sharpcaterpillar.campusecho.item.domain.ItemImage;

public record ItemImageResponse(
        Long id,
        String url,
        int sortOrder,
        boolean primary,
        Instant createdAt) {

    public static ItemImageResponse from(ItemImage image) {
        return new ItemImageResponse(
                image.getId(),
                "/api/v1/item-images/" + image.getId() + "/content",
                image.getSortOrder(),
                image.isPrimary(),
                image.getCreatedAt());
    }
}
