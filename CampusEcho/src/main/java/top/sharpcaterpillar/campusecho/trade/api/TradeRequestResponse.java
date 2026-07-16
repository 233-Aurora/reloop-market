package top.sharpcaterpillar.campusecho.trade.api;

import java.time.Instant;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequest;
import top.sharpcaterpillar.campusecho.trade.domain.TradeStatus;

public record TradeRequestResponse(
        Long id,
        Long itemId,
        String itemTitle,
        Long requesterId,
        Long sellerId,
        String message,
        TradeStatus status,
        boolean requesterConfirmed,
        boolean sellerConfirmed,
        Instant createdAt,
        Instant updatedAt) {

    public static TradeRequestResponse from(TradeRequest request, MarketplaceItem item) {
        return new TradeRequestResponse(
                request.getId(),
                request.getItemId(),
                item == null ? null : item.getTitle(),
                request.getRequesterId(),
                request.getSellerId(),
                request.getMessage(),
                request.getStatus(),
                request.getRequesterConfirmedAt() != null,
                request.getSellerConfirmedAt() != null,
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
