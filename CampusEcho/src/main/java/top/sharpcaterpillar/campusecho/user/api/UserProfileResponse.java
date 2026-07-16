package top.sharpcaterpillar.campusecho.user.api;

import java.math.BigDecimal;
import java.time.Instant;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;

public record UserProfileResponse(
        Long id,
        String username,
        String displayName,
        String campus,
        String avatarUrl,
        BigDecimal reputationScore,
        Instant createdAt) {

    public static UserProfileResponse from(UserAccount user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getCampus(),
                user.getAvatarUrl(),
                user.getReputationScore(),
                user.getCreatedAt());
    }
}
