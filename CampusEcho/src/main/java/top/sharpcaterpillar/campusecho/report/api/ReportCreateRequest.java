package top.sharpcaterpillar.campusecho.report.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import top.sharpcaterpillar.campusecho.report.domain.ReportReason;
import top.sharpcaterpillar.campusecho.report.domain.ReportTargetType;

public record ReportCreateRequest(
        @NotNull(message = "举报目标类型不能为空") ReportTargetType targetType,
        @NotNull(message = "举报目标不能为空") @Positive(message = "举报目标不合法") Long targetId,
        @NotNull(message = "举报原因不能为空") ReportReason reason,
        @NotBlank(message = "举报说明不能为空")
        @Size(min = 10, max = 1000, message = "举报说明长度必须在 10 到 1000 个字符之间") String description) {
}
