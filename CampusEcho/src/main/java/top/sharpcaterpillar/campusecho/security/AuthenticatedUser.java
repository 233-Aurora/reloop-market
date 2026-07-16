package top.sharpcaterpillar.campusecho.security;

import top.sharpcaterpillar.campusecho.user.domain.UserRole;

public record AuthenticatedUser(Long id, String username, UserRole role) {
}
