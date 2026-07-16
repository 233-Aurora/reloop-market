package top.sharpcaterpillar.campusecho.user.application;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.user.api.RegisterRequest;
import top.sharpcaterpillar.campusecho.user.api.UserProfileResponse;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;

@Service
public class UserRegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "用户名已被使用");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "邮箱已被使用");
        }

        UserAccount user = UserAccount.register(
                username,
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                normalizeOptional(request.phone()),
                normalizeOptional(request.campus()));
        return UserProfileResponse.from(userAccountRepository.save(user));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
