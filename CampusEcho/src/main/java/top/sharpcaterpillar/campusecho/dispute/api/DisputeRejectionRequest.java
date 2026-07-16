package top.sharpcaterpillar.campusecho.dispute.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisputeRejectionRequest(
        @NotBlank(message = "驳回说明不能为空")
        @Size(min = 2, max = 1000, message = "驳回说明长度必须在 2 到 1000 个字符之间") String resolutionNote) {
}
