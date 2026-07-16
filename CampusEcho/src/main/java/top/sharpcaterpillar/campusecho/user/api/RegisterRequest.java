package top.sharpcaterpillar.campusecho.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[A-Za-z0-9_]{3,32}", message = "用户名只能由 3-32 位字母、数字或下划线组成")
        String username,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱长度不能超过 254 个字符")
        String email,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为 8-72 位")
        String password,
        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称长度不能超过 64 个字符")
        String displayName,
        @Size(max = 32, message = "手机号长度不能超过 32 个字符")
        String phone,
        @Size(max = 128, message = "校区长度不能超过 128 个字符")
        String campus) {
}
