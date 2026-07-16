package top.sharpcaterpillar.campusecho.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "登录标识不能为空")
        @Size(max = 254, message = "登录标识长度不能超过 254 个字符")
        String login,
        @NotBlank(message = "密码不能为空")
        @Size(max = 72, message = "密码长度不能超过 72 位")
        String password) {
}
