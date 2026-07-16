package top.sharpcaterpillar.campusecho.report.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemRestoreRequest(
        @NotBlank(message = "恢复说明不能为空")
        @Size(min = 2, max = 1000, message = "恢复说明长度必须在 2 到 1000 个字符之间") String reason) {
}
