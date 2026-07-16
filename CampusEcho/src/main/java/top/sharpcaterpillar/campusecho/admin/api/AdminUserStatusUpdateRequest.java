package top.sharpcaterpillar.campusecho.admin.api;

import jakarta.validation.constraints.NotNull;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

public record AdminUserStatusUpdateRequest(
        @NotNull(message = "用户状态不能为空") UserStatus status) {
}
