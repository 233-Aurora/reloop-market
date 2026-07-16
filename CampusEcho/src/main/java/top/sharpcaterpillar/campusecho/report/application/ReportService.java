package top.sharpcaterpillar.campusecho.report.application;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.admin.application.AdminAccessService;
import top.sharpcaterpillar.campusecho.admin.application.AdminService;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.message.application.NotificationService;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.report.api.ItemModerationResponse;
import top.sharpcaterpillar.campusecho.report.api.ReportCreateRequest;
import top.sharpcaterpillar.campusecho.report.api.ReportResponse;
import top.sharpcaterpillar.campusecho.report.domain.ModerationReport;
import top.sharpcaterpillar.campusecho.report.domain.ModerationReportRepository;
import top.sharpcaterpillar.campusecho.report.domain.ReportAction;
import top.sharpcaterpillar.campusecho.report.domain.ReportReason;
import top.sharpcaterpillar.campusecho.report.domain.ReportStatus;
import top.sharpcaterpillar.campusecho.report.domain.ReportTargetType;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class ReportService {

    private static final Set<ReportReason> ITEM_REASONS = EnumSet.of(
            ReportReason.FRAUD,
            ReportReason.PROHIBITED_ITEM,
            ReportReason.MISLEADING_INFORMATION,
            ReportReason.OTHER);
    private static final Set<ReportReason> USER_REASONS = EnumSet.of(
            ReportReason.FRAUD,
            ReportReason.HARASSMENT,
            ReportReason.IMPERSONATION,
            ReportReason.OTHER);

    private final ModerationReportRepository reportRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final AdminAccessService adminAccessService;
    private final AdminService adminService;
    private final GovernanceTradeService governanceTradeService;
    private final NotificationService notificationService;

    public ReportService(
            ModerationReportRepository reportRepository,
            MarketplaceItemRepository marketplaceItemRepository,
            UserAccountRepository userAccountRepository,
            AdminAccessService adminAccessService,
            AdminService adminService,
            GovernanceTradeService governanceTradeService,
            NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.adminAccessService = adminAccessService;
        this.adminService = adminService;
        this.governanceTradeService = governanceTradeService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReportResponse create(AuthenticatedUser currentUser, ReportCreateRequest request) {
        requireActiveUser(currentUser.id());
        validateReason(request.targetType(), request.reason());
        String description = normalizeText(request.description(), 10, "举报说明长度必须在 10 到 1000 个字符之间");
        ModerationReport report = switch (request.targetType()) {
            case ITEM -> createItemReport(currentUser.id(), request, description);
            case USER -> createUserReport(currentUser.id(), request, description);
        };
        try {
            return response(reportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "已经提交过待处理的举报");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listMine(
            AuthenticatedUser currentUser,
            ReportTargetType targetType,
            ReportStatus status,
            int page,
            int size) {
        requireActiveUser(currentUser.id());
        Page<ModerationReport> reports = reportRepository.searchMine(
                currentUser.id(), targetType, status, pageable(page, size));
        return PageResponse.from(reports.map(this::response));
    }

    @Transactional(readOnly = true)
    public ReportResponse detailMine(Long reportId, AuthenticatedUser currentUser) {
        requireActiveUser(currentUser.id());
        return response(reportRepository.findByIdAndReporterId(reportId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "举报不存在")));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listForAdmin(
            AuthenticatedUser currentUser,
            ReportTargetType targetType,
            ReportReason reason,
            ReportStatus status,
            int page,
            int size) {
        adminAccessService.requireAdmin(currentUser);
        Page<ModerationReport> reports = reportRepository.searchForAdmin(
                targetType, reason, status, pageable(page, size));
        return PageResponse.from(reports.map(this::response));
    }

    @Transactional(readOnly = true)
    public ReportResponse detailForAdmin(Long reportId, AuthenticatedUser currentUser) {
        adminAccessService.requireAdmin(currentUser);
        return response(reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "举报不存在")));
    }

    @Transactional
    public ReportResponse resolve(
            Long reportId,
            ReportAction action,
            String resolutionNote,
            AuthenticatedUser currentUser) {
        UserAccount administrator = adminAccessService.requireAdmin(currentUser);
        ModerationReport report = pendingReportForUpdate(reportId);
        validateAction(report.getTargetType(), action);
        String note = normalizeText(resolutionNote, 2, "处理说明长度必须在 2 到 1000 个字符之间");
        Instant now = Instant.now();

        if (blocksItem(action)) {
            blockReportedItem(report, note, administrator.getId(), now);
        }
        if (suspendsUser(action)) {
            adminService.changeStatus(reportedUserId(report), UserStatus.SUSPENDED, currentUser);
        }
        report.resolve(action, note, administrator.getId(), now);
        notificationService.notify(
                report.getReporterId(),
                administrator.getId(),
                null,
                report.getId(),
                report.getItemId(),
                MessageType.REPORT_RESOLVED,
                "你的举报已处理完成。处理说明：" + note);
        return response(report);
    }

    @Transactional
    public ReportResponse reject(Long reportId, String resolutionNote, AuthenticatedUser currentUser) {
        UserAccount administrator = adminAccessService.requireAdmin(currentUser);
        ModerationReport report = pendingReportForUpdate(reportId);
        String note = normalizeText(resolutionNote, 2, "驳回说明长度必须在 2 到 1000 个字符之间");
        report.reject(note, administrator.getId(), Instant.now());
        notificationService.notify(
                report.getReporterId(),
                administrator.getId(),
                null,
                report.getId(),
                report.getItemId(),
                MessageType.REPORT_REJECTED,
                "你的举报已被驳回。说明：" + note);
        return response(report);
    }

    @Transactional
    public ItemModerationResponse restoreItem(
            Long itemId, String reason, AuthenticatedUser currentUser) {
        UserAccount administrator = adminAccessService.requireAdmin(currentUser);
        MarketplaceItem item = marketplaceItemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "物品不存在"));
        if (item.getModerationStatus() != ItemModerationStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "物品当前没有被管理员屏蔽");
        }
        String note = normalizeText(reason, 2, "恢复说明长度必须在 2 到 1000 个字符之间");
        item.restoreModeration(note, administrator.getId(), Instant.now());
        notificationService.notify(
                item.getSellerId(),
                administrator.getId(),
                null,
                null,
                item.getId(),
                MessageType.ITEM_RESTORED,
                "你的物品《" + item.getTitle() + "》已解除屏蔽，可修改后重新上架。说明：" + note);
        return ItemModerationResponse.from(item);
    }

    private ModerationReport createItemReport(
            Long reporterId, ReportCreateRequest request, String description) {
        MarketplaceItem item = marketplaceItemRepository.findById(request.targetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "举报物品不存在"));
        if (item.getSellerId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能举报自己的物品");
        }
        if (item.getModerationStatus() == ItemModerationStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "物品已经被管理员屏蔽");
        }
        if (reportRepository.existsByReporterIdAndItemIdAndStatus(
                reporterId, item.getId(), ReportStatus.PENDING)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "已经提交过该物品的待处理举报");
        }
        return ModerationReport.forItem(
                reporterId, item.getId(), request.reason(), description);
    }

    private ModerationReport createUserReport(
            Long reporterId, ReportCreateRequest request, String description) {
        if (reporterId.equals(request.targetId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能举报自己");
        }
        UserAccount target = userAccountRepository.findById(request.targetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "举报用户不存在"));
        if (target.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "举报用户已经被停用");
        }
        if (reportRepository.existsByReporterIdAndReportedUserIdAndStatus(
                reporterId, target.getId(), ReportStatus.PENDING)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "已经提交过该用户的待处理举报");
        }
        return ModerationReport.forUser(
                reporterId, target.getId(), request.reason(), description);
    }

    private void blockReportedItem(
            ModerationReport report, String note, Long administratorId, Instant now) {
        if (report.getTargetType() != ReportTargetType.ITEM) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前举报不能屏蔽物品");
        }
        if (governanceTradeService.blockItem(report.getItemId(), note, administratorId, now) == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "举报物品不存在");
        }
    }

    private Long reportedUserId(ModerationReport report) {
        if (report.getTargetType() == ReportTargetType.USER) {
            return report.getReportedUserId();
        }
        return marketplaceItemRepository.findById(report.getItemId())
                .map(MarketplaceItem::getSellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "举报物品不存在"));
    }

    private ModerationReport pendingReportForUpdate(Long reportId) {
        ModerationReport report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "举报不存在"));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "举报已经处理，不能重复操作");
        }
        return report;
    }

    private void validateReason(ReportTargetType targetType, ReportReason reason) {
        Set<ReportReason> allowed = targetType == ReportTargetType.ITEM ? ITEM_REASONS : USER_REASONS;
        if (!allowed.contains(reason)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "举报原因不适用于当前目标类型");
        }
    }

    private void validateAction(ReportTargetType targetType, ReportAction action) {
        if (targetType == ReportTargetType.USER
                && action != ReportAction.NONE
                && action != ReportAction.SUSPEND_USER) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "用户举报不支持该处理动作");
        }
    }

    private boolean blocksItem(ReportAction action) {
        return action == ReportAction.BLOCK_ITEM || action == ReportAction.BLOCK_ITEM_AND_SUSPEND_USER;
    }

    private boolean suspendsUser(ReportAction action) {
        return action == ReportAction.SUSPEND_USER || action == ReportAction.BLOCK_ITEM_AND_SUSPEND_USER;
    }

    private ReportResponse response(ModerationReport report) {
        UserAccount reporter = userAccountRepository.findById(report.getReporterId()).orElse(null);
        UserAccount handler = report.getHandledBy() == null
                ? null
                : userAccountRepository.findById(report.getHandledBy()).orElse(null);
        Long targetId;
        String targetLabel;
        Long targetOwnerId;
        if (report.getTargetType() == ReportTargetType.ITEM) {
            MarketplaceItem item = marketplaceItemRepository.findById(report.getItemId()).orElse(null);
            targetId = report.getItemId();
            targetLabel = item == null ? null : item.getTitle();
            targetOwnerId = item == null ? null : item.getSellerId();
        } else {
            UserAccount target = userAccountRepository.findById(report.getReportedUserId()).orElse(null);
            targetId = report.getReportedUserId();
            targetLabel = target == null ? null : target.getDisplayName();
            targetOwnerId = null;
        }
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                reporter == null ? null : reporter.getUsername(),
                report.getTargetType(),
                targetId,
                targetLabel,
                targetOwnerId,
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getAction(),
                report.getResolutionNote(),
                report.getHandledBy(),
                handler == null ? null : handler.getUsername(),
                report.getHandledAt(),
                report.getCreatedAt(),
                report.getUpdatedAt());
    }

    private void requireActiveUser(Long userId) {
        if (userAccountRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用");
        }
    }

    private String normalizeText(String value, int minLength, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < minLength || normalized.length() > 1000) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, message);
        }
        return normalized;
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
