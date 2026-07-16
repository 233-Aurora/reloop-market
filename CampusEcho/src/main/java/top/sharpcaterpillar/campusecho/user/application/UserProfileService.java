package top.sharpcaterpillar.campusecho.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.api.CurrentUserResponse;
import top.sharpcaterpillar.campusecho.user.api.UserProfileResponse;
import top.sharpcaterpillar.campusecho.user.api.UserProfileUpdateRequest;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class UserProfileService {

    private final UserAccountRepository userAccountRepository;

    public UserProfileService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(Long id) {
        return userAccountRepository.findByIdAndStatus(id, UserStatus.ACTIVE)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(AuthenticatedUser authenticatedUser) {
        return userAccountRepository.findByIdAndStatus(authenticatedUser.id(), UserStatus.ACTIVE)
                .map(CurrentUserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用"));
    }

    @Transactional
    public CurrentUserResponse updateCurrentUser(AuthenticatedUser authenticatedUser, UserProfileUpdateRequest request) {
        var user = userAccountRepository.findByIdAndStatusForUpdate(authenticatedUser.id(), UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用"));
        user.updateProfile(
                request.displayName().trim(),
                normalizeOptional(request.phone()),
                normalizeOptional(request.campus()));
        return CurrentUserResponse.from(user);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
