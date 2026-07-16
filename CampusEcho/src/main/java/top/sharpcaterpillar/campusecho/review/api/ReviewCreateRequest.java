package top.sharpcaterpillar.campusecho.review.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @Min(value = 1, message = "评分不能低于 1")
        @Max(value = 5, message = "评分不能高于 5")
        int rating,
        @Size(max = 1000, message = "评价内容不能超过 1000 个字符")
        String comment) {
}
