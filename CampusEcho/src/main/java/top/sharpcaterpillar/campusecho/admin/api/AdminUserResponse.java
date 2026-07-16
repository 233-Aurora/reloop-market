package top.sharpcaterpillar.campusecho.admin.api;

import java.math.BigDecimal;
import java.time.Instant;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String phone,
        String campus,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        BigDecimal reputationScore,
        Instant createdAt) {

    public static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhone(),
                user.getCampus(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getReputationScore(),
                user.getCreatedAt());
    }
}
