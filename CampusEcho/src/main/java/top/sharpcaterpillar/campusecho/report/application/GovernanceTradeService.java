package top.sharpcaterpillar.campusecho.report.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import org.springframework.stereotype.Service;
import top.sharpcaterpillar.campusecho.dispute.application.DisputeService;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.message.application.NotificationService;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequest;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequestRepository;
import top.sharpcaterpillar.campusecho.trade.domain.TradeStatus;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class GovernanceTradeService {

    private static final List<TradeStatus> OPEN_STATUSES =
            List.of(TradeStatus.PENDING, TradeStatus.ACCEPTED, TradeStatus.DISPUTED);

    private final TradeRequestRepository tradeRequestRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    private final DisputeService disputeService;

    public GovernanceTradeService(
            TradeRequestRepository tradeRequestRepository,
            MarketplaceItemRepository marketplaceItemRepository,
            UserAccountRepository userAccountRepository,
            NotificationService notificationService,
            DisputeService disputeService) {
        this.tradeRequestRepository = tradeRequestRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
        this.disputeService = disputeService;
    }

    public MarketplaceItem blockItem(Long itemId, String reason, Long administratorId, Instant moderatedAt) {
        List<TradeRequest> openRequests = tradeRequestRepository.findByItemIdAndStatusInForUpdate(itemId, OPEN_STATUSES);
        MarketplaceItem item = marketplaceItemRepository.findByIdForUpdate(itemId).orElse(null);
        if (item == null) {
            openRequests.forEach(TradeRequest::cancel);
            return null;
        }
        String itemTitle = item.getTitle();
        List<Long> disputedTradeIds = openRequests.stream()
                .filter(request -> request.getStatus() == TradeStatus.DISPUTED)
                .map(TradeRequest::getId)
                .toList();
        if (!disputedTradeIds.isEmpty()) {
            disputeService.forceCloseOpenDisputes(disputedTradeIds, "物品被管理员屏蔽", administratorId);
        }
        openRequests.forEach(request -> {
            request.cancel();
            notifyTradeCancelledByModeration(request, itemTitle, "物品被管理员屏蔽");
        });
        if (item.getStatus() != ItemStatus.SOLD) {
            item.changeStatus(ItemStatus.OFF_SHELF);
        }
        if (item.getModerationStatus() == ItemModerationStatus.NORMAL) {
            item.block(reason, administratorId, moderatedAt);
            notificationService.notify(
                    item.getSellerId(),
                    administratorId,
                    null,
                    null,
                    item.getId(),
                    MessageType.ITEM_BLOCKED,
                    "你的物品《" + itemTitle + "》已被管理员屏蔽。原因：" + reason);
        }
        return item;
    }

    public void cancelForSuspendedUser(Long userId, Long administratorId) {
        if (administratorId == null) {
            throw new IllegalArgumentException("administratorId is required when cancelling trades for suspended user");
        }
        List<TradeRequest> requests = tradeRequestRepository.findByParticipantAndStatusInForUpdate(
                userId, OPEN_STATUSES);
        List<Long> acceptedItemIds = requests.stream()
                .filter(request -> request.getStatus() == TradeStatus.ACCEPTED
                        || request.getStatus() == TradeStatus.DISPUTED)
                .map(TradeRequest::getItemId)
                .distinct()
                .sorted()
                .toList();
        Map<Long, String> itemTitles = loadItemTitles(requests);
        List<Long> disputedTradeIds = requests.stream()
                .filter(request -> request.getStatus() == TradeStatus.DISPUTED)
                .map(TradeRequest::getId)
                .toList();
        if (!disputedTradeIds.isEmpty()) {
            disputeService.forceCloseOpenDisputes(disputedTradeIds, "参与方账户被停用", administratorId);
        }
        requests.forEach(request -> {
            request.cancel();
            notifyTradeCancelledByModeration(
                    request,
                    itemTitles.getOrDefault(request.getItemId(), "相关物品"),
                    "参与方账户被停用");
        });

        acceptedItemIds.forEach(itemId -> marketplaceItemRepository.findByIdForUpdate(itemId)
                .ifPresent(item -> restoreAfterCancelledTrade(item, userId)));
    }

    private void notifyTradeCancelledByModeration(TradeRequest request, String itemTitle, String cause) {
        String content = "交易《" + itemTitle + "》因" + cause + "已取消。";
        notificationService.notify(
                request.getRequesterId(),
                null,
                request.getId(),
                null,
                request.getItemId(),
                MessageType.TRADE_CANCELLED_BY_MODERATION,
                content);
        notificationService.notify(
                request.getSellerId(),
                null,
                request.getId(),
                null,
                request.getItemId(),
                MessageType.TRADE_CANCELLED_BY_MODERATION,
                content);
    }

    private Map<Long, String> loadItemTitles(List<TradeRequest> requests) {
        Map<Long, String> titles = new HashMap<>();
        requests.stream()
                .map(TradeRequest::getItemId)
                .distinct()
                .forEach(itemId -> marketplaceItemRepository.findById(itemId)
                        .ifPresent(item -> titles.put(itemId, item.getTitle())));
        return titles;
    }

    private void restoreAfterCancelledTrade(MarketplaceItem item, Long suspendedUserId) {
        boolean sellerActive = !item.getSellerId().equals(suspendedUserId)
                && userAccountRepository.findByIdAndStatus(item.getSellerId(), UserStatus.ACTIVE).isPresent();
        if (sellerActive && item.getModerationStatus() == ItemModerationStatus.NORMAL) {
            item.changeStatus(ItemStatus.ON_SALE);
        } else {
            item.changeStatus(ItemStatus.OFF_SHELF);
        }
    }
}
