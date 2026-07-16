package top.sharpcaterpillar.campusecho.report.api;

import java.time.Instant;
import top.sharpcaterpillar.campusecho.report.domain.ReportAction;
import top.sharpcaterpillar.campusecho.report.domain.ReportReason;
import top.sharpcaterpillar.campusecho.report.domain.ReportStatus;
import top.sharpcaterpillar.campusecho.report.domain.ReportTargetType;

public record ReportResponse(
        Long id,
        Long reporterId,
        String reporterUsername,
        ReportTargetType targetType,
        Long targetId,
        String targetLabel,
        Long targetOwnerId,
        ReportReason reason,
        String description,
        ReportStatus status,
        ReportAction action,
        String resolutionNote,
        Long handledBy,
        String handlerUsername,
        Instant handledAt,
        Instant createdAt,
        Instant updatedAt) {
}
