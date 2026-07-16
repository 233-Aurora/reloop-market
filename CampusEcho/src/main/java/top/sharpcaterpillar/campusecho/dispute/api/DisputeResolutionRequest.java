package top.sharpcaterpillar.campusecho.dispute.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeResolution;

public record DisputeResolutionRequest(
        @NotNull(message = "仲裁结果不能为空") DisputeResolution resolution,
        @NotBlank(message = "仲裁说明不能为空")
        @Size(min = 2, max = 1000, message = "仲裁说明长度必须在 2 到 1000 个字符之间") String resolutionNote) {
}
