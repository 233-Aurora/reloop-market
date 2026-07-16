package top.sharpcaterpillar.campusecho.dispute.application;

import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.admin.application.AdminAccessService;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.dispute.api.DisputeCreateRequest;
import top.sharpcaterpillar.campusecho.dispute.api.DisputeResponse;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeReason;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeResolution;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeStatus;
import top.sharpcaterpillar.campusecho.dispute.domain.TradeDispute;
import top.sharpcaterpillar.campusecho.dispute.domain.TradeDisputeRepository;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.message.application.NotificationService;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequest;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequestRepository;
import top.sharpcaterpillar.campusecho.trade.domain.TradeStatus;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class DisputeService {

    private final TradeDisputeRepository tradeDisputeRepository;
    private final TradeRequestRepository tradeRequestRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final AdminAccessService adminAccessService;
    private final NotificationService notificationService;

    public DisputeService(
            TradeDisputeRepository tradeDisputeRepository,
            TradeRequestRepository tradeRequestRepository,
            MarketplaceItemRepository marketplaceItemRepository,
            UserAccountRepository userAccountRepository,
            AdminAccessService adminAccessService,
            NotificationService notificationService) {
        this.tradeDisputeRepository = tradeDisputeRepository;
        this.tradeRequestRepository = tradeRequestRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.adminAccessService = adminAccessService;
        this.notificationService = notificationService;
    }

    @Transactional
    public DisputeResponse open(AuthenticatedUser currentUser, DisputeCreateRequest request) {
        requireActiveUser(currentUser.id());
        String description = normalizeText(request.description(), 10, "争议说明长度必须在 10 到 1000 个字符之间");
        TradeRequest tradeRequest = tradeRequestRepository.findByIdAndParticipant(
                        request.tradeRequestId(), currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "交易请求不存在或无权访问"));
        if (tradeRequest.getStatus() != TradeStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅已接受且未完成的交易可以发起争议");
        }
        if (tradeDisputeRepository.existsByTradeRequestIdAndStatus(tradeRequest.getId(), DisputeStatus.OPEN)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该交易已有待处理争议");
        }
        MarketplaceItem item = marketplaceItemRepository.findById(tradeRequest.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联物品不存在"));
        if (item.getModerationStatus() != ItemModerationStatus.NORMAL) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "物品已被管理员屏蔽，请通过举报处理结果查看");
        }
        Long respondentId = tradeRequest.getRequesterId().equals(currentUser.id())
                ? tradeRequest.getSellerId()
                : tradeRequest.getRequesterId();
        TradeDispute dispute = TradeDispute.open(
                tradeRequest.getId(),
                item.getId(),
                currentUser.id(),
                respondentId,
                request.reason(),
                description);
        try {
            TradeDispute saved = tradeDisputeRepository.saveAndFlush(dispute);
            tradeRequest.markDisputed();
            notifyOpened(saved, item.getTitle());
            return response(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该交易已有待处理争议");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> listMine(
            AuthenticatedUser currentUser,
            DisputeStatus status,
            DisputeReason reason,
            int page,
            int size) {
        requireActiveUser(currentUser.id());
        Page<TradeDispute> disputes = tradeDisputeRepository.searchMine(
                currentUser.id(), status, reason, pageable(page, size));
        return PageResponse.from(disputes.map(this::response));
    }

    @Transactional(readOnly = true)
    public DisputeResponse detailMine(Long disputeId, AuthenticatedUser currentUser) {
        requireActiveUser(currentUser.id());
        return response(tradeDisputeRepository.findByIdAndParticipant(disputeId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "争议不存在或无权访问")));
    }

    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> listForAdmin(
            AuthenticatedUser currentUser,
            DisputeStatus status,
            DisputeReason reason,
            int page,
            int size) {
        adminAccessService.requireAdmin(currentUser);
        Page<TradeDispute> disputes = tradeDisputeRepository.searchForAdmin(status, reason, pageable(page, size));
        return PageResponse.from(disputes.map(this::response));
    }

    @Transactional(readOnly = true)
    public DisputeResponse detailForAdmin(Long disputeId, AuthenticatedUser currentUser) {
        adminAccessService.requireAdmin(currentUser);
        return response(tradeDisputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "争议不存在")));
    }

    @Transactional
    public DisputeResponse resolve(
            Long disputeId,
            DisputeResolution resolution,
            String resolutionNote,
            AuthenticatedUser currentUser) {
        UserAccount administrator = adminAccessService.requireAdmin(currentUser);
        if (resolution == DisputeResolution.NONE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "处理争议时不能使用 NONE，请使用驳回接口");
        }
        TradeDispute dispute = openDisputeForUpdate(disputeId);
        String note = normalizeText(resolutionNote, 2, "仲裁说明长度必须在 2 到 1000 个字符之间");
        Instant now = Instant.now();
        TradeRequest tradeRequest = tradeRequestRepository.findById(dispute.getTradeRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联交易不存在"));
        if (tradeRequest.getStatus() != TradeStatus.DISPUTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "关联交易当前状态不允许仲裁");
        }
        MarketplaceItem item = marketplaceItemRepository.findByIdForUpdate(dispute.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联物品不存在"));

        if (resolution == DisputeResolution.COMPLETE_TRADE) {
            tradeRequest.completeFromDispute(now);
            if (item.getStatus() != ItemStatus.SOLD) {
                item.changeStatus(ItemStatus.SOLD);
            }
        } else if (resolution == DisputeResolution.CANCEL_TRADE) {
            tradeRequest.cancelFromDispute();
            restoreItemAfterCancelledDispute(item);
        }

        dispute.resolve(resolution, note, administrator.getId(), now);
        notifyResolved(dispute, item.getTitle(), resolution, note, administrator.getId());
        return response(dispute);
    }

    @Transactional
    public DisputeResponse reject(Long disputeId, String resolutionNote, AuthenticatedUser currentUser) {
        UserAccount administrator = adminAccessService.requireAdmin(currentUser);
        TradeDispute dispute = openDisputeForUpdate(disputeId);
        String note = normalizeText(resolutionNote, 2, "驳回说明长度必须在 2 到 1000 个字符之间");
        Instant now = Instant.now();
        TradeRequest tradeRequest = tradeRequestRepository.findById(dispute.getTradeRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联交易不存在"));
        if (tradeRequest.getStatus() != TradeStatus.DISPUTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "关联交易当前状态不允许驳回争议");
        }
        MarketplaceItem item = marketplaceItemRepository.findById(dispute.getItemId()).orElse(null);
        tradeRequest.reopenAfterDisputeRejected();
        dispute.reject(note, administrator.getId(), now);
        notifyRejected(dispute, item == null ? "相关物品" : item.getTitle(), note, administrator.getId());
        return response(dispute);
    }

    @Transactional
    public void forceCloseOpenDisputes(List<Long> tradeRequestIds, String cause, Long actorId) {
        if (tradeRequestIds == null || tradeRequestIds.isEmpty()) {
            return;
        }
        if (actorId == null) {
            throw new IllegalArgumentException("actorId is required when closing disputes");
        }
        List<TradeDispute> openDisputes = tradeDisputeRepository.findByTradeRequestIdInAndStatusForUpdate(
                tradeRequestIds, DisputeStatus.OPEN);
        Instant now = Instant.now();
        for (TradeDispute dispute : openDisputes) {
            String itemTitle = marketplaceItemRepository.findById(dispute.getItemId())
                    .map(MarketplaceItem::getTitle)
                    .orElse("相关物品");
            String note = "系统因" + cause + "自动关闭争议。";
            dispute.reject(note, actorId, now);
            notificationService.notify(
                    dispute.getOpenerId(),
                    actorId,
                    dispute.getTradeRequestId(),
                    null,
                    dispute.getId(),
                    dispute.getItemId(),
                    MessageType.DISPUTE_REJECTED,
                    "关于《" + itemTitle + "》的争议已自动关闭。说明：" + note);
            notificationService.notify(
                    dispute.getRespondentId(),
                    actorId,
                    dispute.getTradeRequestId(),
                    null,
                    dispute.getId(),
                    dispute.getItemId(),
                    MessageType.DISPUTE_REJECTED,
                    "关于《" + itemTitle + "》的争议已自动关闭。说明：" + note);
        }
    }

    private void restoreItemAfterCancelledDispute(MarketplaceItem item) {
        if (item.getStatus() == ItemStatus.SOLD) {
            return;
        }
        boolean sellerActive = userAccountRepository
                .findByIdAndStatus(item.getSellerId(), UserStatus.ACTIVE)
                .isPresent();
        if (sellerActive && item.getModerationStatus() == ItemModerationStatus.NORMAL) {
            item.changeStatus(ItemStatus.ON_SALE);
        } else {
            item.changeStatus(ItemStatus.OFF_SHELF);
        }
    }

    private void notifyOpened(TradeDispute dispute, String itemTitle) {
        notificationService.notify(
                dispute.getRespondentId(),
                dispute.getOpenerId(),
                dispute.getTradeRequestId(),
                null,
                dispute.getId(),
                dispute.getItemId(),
                MessageType.DISPUTE_OPENED,
                "交易《" + itemTitle + "》已进入争议，等待管理员仲裁。");
        notificationService.notify(
                dispute.getOpenerId(),
                null,
                dispute.getTradeRequestId(),
                null,
                dispute.getId(),
                dispute.getItemId(),
                MessageType.DISPUTE_OPENED,
                "你已对交易《" + itemTitle + "》发起争议，等待管理员仲裁。");
    }

    private void notifyResolved(
            TradeDispute dispute,
            String itemTitle,
            DisputeResolution resolution,
            String note,
            Long administratorId) {
        String resultText = resolution == DisputeResolution.COMPLETE_TRADE ? "裁定交易完成" : "裁定取消交易";
        String content = "关于《" + itemTitle + "》的争议已处理：" + resultText + "。说明：" + note;
        notificationService.notify(
                dispute.getOpenerId(),
                administratorId,
                dispute.getTradeRequestId(),
                null,
                dispute.getId(),
                dispute.getItemId(),
                MessageType.DISPUTE_RESOLVED,
                content);
        notificationService.notify(
                dispute.getRespondentId(),
                administratorId,
                dispute.getTradeRequestId(),
                null,
                dispute.getId(),
                dispute.getItemId(),
                MessageType.DISPUTE_RESOLVED,
                content);
    }

    private void notifyRejected(TradeDispute dispute, String itemTitle, String note, Long administratorId) {
        String content = "关于《" + itemTitle + "》的争议已被驳回，交易恢复为已接受。说明：" + note;
        notificationService.notify(
                dispute.getOpenerId(),
                administratorId,
                dispute.getTradeRequestId(),
                null,
                dispute.getId(),
                dispute.getItemId(),
                MessageType.DISPUTE_REJECTED,
                content);
        notificationService.notify(
                dispute.getRespondentId(),
                administratorId,
                dispute.getTradeRequestId(),
                null,
                dispute.getId(),
                dispute.getItemId(),
                MessageType.DISPUTE_REJECTED,
                content);
    }

    private TradeDispute openDisputeForUpdate(Long disputeId) {
        TradeDispute dispute = tradeDisputeRepository.findByIdForUpdate(disputeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "争议不存在"));
        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "争议已经处理，不能重复操作");
        }
        return dispute;
    }

    private DisputeResponse response(TradeDispute dispute) {
        UserAccount opener = userAccountRepository.findById(dispute.getOpenerId()).orElse(null);
        UserAccount respondent = userAccountRepository.findById(dispute.getRespondentId()).orElse(null);
        UserAccount handler = dispute.getHandledBy() == null
                ? null
                : userAccountRepository.findById(dispute.getHandledBy()).orElse(null);
        MarketplaceItem item = marketplaceItemRepository.findById(dispute.getItemId()).orElse(null);
        return new DisputeResponse(
                dispute.getId(),
                dispute.getTradeRequestId(),
                dispute.getItemId(),
                item == null ? null : item.getTitle(),
                dispute.getOpenerId(),
                opener == null ? null : opener.getUsername(),
                dispute.getRespondentId(),
                respondent == null ? null : respondent.getUsername(),
                dispute.getReason(),
                dispute.getDescription(),
                dispute.getStatus(),
                dispute.getResolution(),
                dispute.getResolutionNote(),
                dispute.getHandledBy(),
                handler == null ? null : handler.getUsername(),
                dispute.getHandledAt(),
                dispute.getCreatedAt(),
                dispute.getUpdatedAt());
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
