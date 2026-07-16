package top.sharpcaterpillar.campusecho.user.api;

import java.math.BigDecimal;
import java.time.Instant;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String phone,
        String campus,
        String avatarUrl,
        UserRole role,
        BigDecimal reputationScore,
        Instant createdAt) {

    public static CurrentUserResponse from(UserAccount user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhone(),
                user.getCampus(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getReputationScore(),
                user.getCreatedAt());
    }
}
