package top.sharpcaterpillar.campusecho.report.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import top.sharpcaterpillar.campusecho.report.domain.ReportAction;

public record ReportResolutionRequest(
        @NotNull(message = "处理动作不能为空") ReportAction action,
        @NotBlank(message = "处理说明不能为空")
        @Size(min = 2, max = 1000, message = "处理说明长度必须在 2 到 1000 个字符之间") String resolutionNote) {
}
