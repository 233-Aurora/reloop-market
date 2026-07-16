package top.sharpcaterpillar.campusecho.admin.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.admin.api.AdminDashboardResponse;
import top.sharpcaterpillar.campusecho.admin.api.AdminUserResponse;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeStatus;
import top.sharpcaterpillar.campusecho.dispute.domain.TradeDisputeRepository;
import top.sharpcaterpillar.campusecho.message.application.NotificationService;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.review.domain.ReviewRepository;
import top.sharpcaterpillar.campusecho.report.application.GovernanceTradeService;
import top.sharpcaterpillar.campusecho.report.domain.ModerationReportRepository;
import top.sharpcaterpillar.campusecho.report.domain.ReportStatus;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequestRepository;
import top.sharpcaterpillar.campusecho.trade.domain.TradeStatus;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class AdminService {

    private final AdminAccessService adminAccessService;
    private final UserAccountRepository userAccountRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final TradeRequestRepository tradeRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ModerationReportRepository reportRepository;
    private final TradeDisputeRepository tradeDisputeRepository;
    private final GovernanceTradeService governanceTradeService;
    private final NotificationService notificationService;

    public AdminService(
            AdminAccessService adminAccessService,
            UserAccountRepository userAccountRepository,
            MarketplaceItemRepository marketplaceItemRepository,
            TradeRequestRepository tradeRequestRepository,
            ReviewRepository reviewRepository,
            ModerationReportRepository reportRepository,
            TradeDisputeRepository tradeDisputeRepository,
            GovernanceTradeService governanceTradeService,
            NotificationService notificationService) {
        this.adminAccessService = adminAccessService;
        this.userAccountRepository = userAccountRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.tradeRequestRepository = tradeRequestRepository;
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.tradeDisputeRepository = tradeDisputeRepository;
        this.governanceTradeService = governanceTradeService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard(AuthenticatedUser currentUser) {
        adminAccessService.requireAdmin(currentUser);
        return new AdminDashboardResponse(
                userAccountRepository.count(),
                userAccountRepository.countByStatus(UserStatus.ACTIVE),
                userAccountRepository.countByStatus(UserStatus.SUSPENDED),
                userAccountRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE),
                marketplaceItemRepository.count(),
                marketplaceItemRepository.countByStatus(ItemStatus.ON_SALE),
                tradeRequestRepository.count(),
                tradeRequestRepository.countByStatus(TradeStatus.PENDING),
                tradeRequestRepository.countByStatus(TradeStatus.COMPLETED),
                reviewRepository.count(),
                reportRepository.count(),
                reportRepository.countByStatus(ReportStatus.PENDING),
                tradeDisputeRepository.count(),
                tradeDisputeRepository.countByStatus(DisputeStatus.OPEN));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(
            AuthenticatedUser currentUser,
            String keyword,
            UserRole role,
            UserStatus status,
            int page,
            int size) {
        adminAccessService.requireAdmin(currentUser);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? "" : keyword.trim();
        Page<UserAccount> users = userAccountRepository.searchForAdmin(
                normalizedKeyword, role, status, pageable(page, size));
        return PageResponse.from(users.map(AdminUserResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse userDetail(Long userId, AuthenticatedUser currentUser) {
        adminAccessService.requireAdmin(currentUser);
        return AdminUserResponse.from(userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在")));
    }

    @Transactional
    public AdminUserResponse changeStatus(Long userId, UserStatus status, AuthenticatedUser currentUser) {
        UserAccount administrator = adminAccessService.requireAdmin(currentUser);
        int activeAdminCount = lockActiveAdmins();
        UserAccount target = userForUpdate(userId);
        if (administrator.getId().equals(target.getId()) && status != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能停用当前管理员账户");
        }
        if (target.getRole() == UserRole.ADMIN
                && target.getStatus() == UserStatus.ACTIVE
                && status == UserStatus.SUSPENDED) {
            requireAnotherActiveAdmin(activeAdminCount);
        }
        if (target.getStatus() != status) {
            target.changeStatus(status);
            if (status == UserStatus.SUSPENDED) {
                governanceTradeService.cancelForSuspendedUser(target.getId(), administrator.getId());
                notificationService.notify(
                        target.getId(),
                        administrator.getId(),
                        null,
                        null,
                        null,
                        MessageType.ACCOUNT_SUSPENDED,
                        "你的账户已被管理员停用，相关未完成交易已取消。");
            }
        }
        return AdminUserResponse.from(target);
    }

    @Transactional
    public AdminUserResponse changeRole(Long userId, UserRole role, AuthenticatedUser currentUser) {
        UserAccount administrator = adminAccessService.requireAdmin(currentUser);
        int activeAdminCount = lockActiveAdmins();
        UserAccount target = userForUpdate(userId);
        if (administrator.getId().equals(target.getId()) && role != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能降级当前管理员账户");
        }
        if (target.getRole() == UserRole.ADMIN
                && target.getStatus() == UserStatus.ACTIVE
                && role == UserRole.USER) {
            requireAnotherActiveAdmin(activeAdminCount);
        }
        target.changeRole(role);
        return AdminUserResponse.from(target);
    }

    private int lockActiveAdmins() {
        return userAccountRepository.findByRoleAndStatusForUpdate(UserRole.ADMIN, UserStatus.ACTIVE).size();
    }

    private void requireAnotherActiveAdmin(int activeAdminCount) {
        if (activeAdminCount <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "系统必须保留至少一个活跃管理员");
        }
    }

    private UserAccount userForUpdate(Long userId) {
        return userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
