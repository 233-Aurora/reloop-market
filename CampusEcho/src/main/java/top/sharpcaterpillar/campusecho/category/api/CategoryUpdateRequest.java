package top.sharpcaterpillar.campusecho.category.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称不能超过 64 个字符")
        String name,
        Long parentId,
        @Min(value = 0, message = "排序值不能小于 0")
        int sortOrder) {
}
