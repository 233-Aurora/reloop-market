package top.sharpcaterpillar.campusecho.user.api;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, CurrentUserResponse user) {
}
