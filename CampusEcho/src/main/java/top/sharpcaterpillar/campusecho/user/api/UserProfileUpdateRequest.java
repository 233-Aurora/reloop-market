package top.sharpcaterpillar.campusecho.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称长度不能超过 64 个字符")
        String displayName,
        @Size(max = 32, message = "手机号长度不能超过 32 个字符")
        String phone,
        @Size(max = 128, message = "校区长度不能超过 128 个字符")
        String campus) {
}
