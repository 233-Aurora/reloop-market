package top.sharpcaterpillar.campusecho.dispute.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeReason;

public record DisputeCreateRequest(
        @NotNull(message = "交易请求不能为空") @Positive(message = "交易请求不合法") Long tradeRequestId,
        @NotNull(message = "争议原因不能为空") DisputeReason reason,
        @NotBlank(message = "争议说明不能为空")
        @Size(min = 10, max = 1000, message = "争议说明长度必须在 10 到 1000 个字符之间") String description) {
}
