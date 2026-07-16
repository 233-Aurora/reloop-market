package top.sharpcaterpillar.campusecho.dispute.api;

import java.time.Instant;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeReason;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeResolution;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeStatus;

public record DisputeResponse(
        Long id,
        Long tradeRequestId,
        Long itemId,
        String itemTitle,
        Long openerId,
        String openerUsername,
        Long respondentId,
        String respondentUsername,
        DisputeReason reason,
        String description,
        DisputeStatus status,
        DisputeResolution resolution,
        String resolutionNote,
        Long handledBy,
        String handlerUsername,
        Instant handledAt,
        Instant createdAt,
        Instant updatedAt) {
}
