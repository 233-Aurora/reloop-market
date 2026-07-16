package top.sharpcaterpillar.campusecho.admin.api;

import jakarta.validation.constraints.NotNull;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;

public record AdminUserRoleUpdateRequest(
        @NotNull(message = "用户角色不能为空") UserRole role) {
}
