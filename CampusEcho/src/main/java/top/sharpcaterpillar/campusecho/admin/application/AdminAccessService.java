package top.sharpcaterpillar.campusecho.admin.application;

import org.springframework.stereotype.Service;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class AdminAccessService {

    private final UserAccountRepository userAccountRepository;

    public AdminAccessService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount requireAdmin(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以执行此操作");
        }
        return userAccountRepository.findByIdAndStatus(currentUser.id(), UserStatus.ACTIVE)
                .filter(account -> account.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "管理员账户不可用"));
    }
}
