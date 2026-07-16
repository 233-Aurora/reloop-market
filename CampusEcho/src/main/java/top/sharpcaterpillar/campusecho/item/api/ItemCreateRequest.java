package top.sharpcaterpillar.campusecho.item.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import top.sharpcaterpillar.campusecho.item.domain.ItemCondition;

public record ItemCreateRequest(
        @NotNull(message = "分类不能为空") Long categoryId,
        @NotBlank(message = "标题不能为空") @Size(max = 120, message = "标题长度不能超过 120 个字符") String title,
        @NotBlank(message = "描述不能为空") @Size(max = 10000, message = "描述长度不能超过 10000 个字符") String description,
        @NotNull(message = "价格不能为空") @DecimalMin(value = "0.00", message = "价格不能小于 0")
        @Digits(integer = 10, fraction = 2, message = "价格最多 10 位整数和 2 位小数") BigDecimal price,
        @NotNull(message = "成色不能为空") ItemCondition condition,
        @Size(max = 255, message = "地点长度不能超过 255 个字符") String locationText) {
}
