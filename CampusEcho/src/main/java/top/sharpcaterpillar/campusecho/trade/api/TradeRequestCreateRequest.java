package top.sharpcaterpillar.campusecho.trade.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TradeRequestCreateRequest(
        @NotNull(message = "物品不能为空") Long itemId,
        @Size(max = 500, message = "留言长度不能超过 500 个字符") String message) {
}
