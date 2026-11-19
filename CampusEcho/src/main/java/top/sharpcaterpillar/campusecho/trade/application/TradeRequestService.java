package top.sharpcaterpillar.campusecho.trade.application;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.message.application.NotificationService;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.trade.api.TradeRequestCreateRequest;
import top.sharpcaterpillar.campusecho.trade.api.TradeRequestResponse;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequest;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequestRepository;
import top.sharpcaterpillar.campusecho.trade.domain.TradeStatus;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class TradeRequestService {

    private final TradeRequestRepository tradeRequestRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    public TradeRequestService(
            TradeRequestRepository tradeRequestRepository,
            MarketplaceItemRepository marketplaceItemRepository,
            UserAccountRepository userAccountRepository,
            NotificationService notificationService) {
        this.tradeRequestRepository = tradeRequestRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public TradeRequestResponse create(AuthenticatedUser currentUser, TradeRequestCreateRequest request) {
        requireActiveUser(currentUser.id());
        MarketplaceItem item = marketplaceItemRepository.findById(request.itemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "物品不存在"));
        if (item.getStatus() != ItemStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前物品不可发起交易");
        }
        requireTradableItem(item);
        if (item.getSellerId().equals(currentUser.id())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能向自己的物品发起交易");
        }
        if (tradeRequestRepository.existsByItemIdAndRequesterIdAndStatusIn(
                item.getId(), currentUser.id(), List.of(TradeStatus.PENDING, TradeStatus.ACCEPTED))) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "已有进行中的交易请求");
        }
        TradeRequest tradeRequest = TradeRequest.create(
                item.getId(),
                currentUser.id(),
                item.getSellerId(),
                normalizeOptional(request.message()));
        TradeRequest saved = tradeRequestRepository.save(tradeRequest);
        notificationService.notify(
                saved.getSellerId(),
                currentUser.id(),
                saved.getId(),
                null,
                item.getId(),
                MessageType.TRADE_REQUEST_CREATED,
                "收到新的交易请求：《" + item.getTitle() + "》。");
        return toResponse(saved, item);
    }

    @Transactional(readOnly = true)
    public PageResponse<TradeRequestResponse> listMine(AuthenticatedUser currentUser, int page, int size) {
        requireActiveUser(currentUser.id());
        Page<TradeRequest> requests = tradeRequestRepository.findMine(currentUser.id(), pageable(page, size));
        return PageResponse.from(requests.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TradeRequestResponse detail(Long id, AuthenticatedUser currentUser) {
        TradeRequest request = participantRequest(id, currentUser.id());
        return toResponse(request);
    }

    @Transactional
    public TradeRequestResponse accept(Long id, AuthenticatedUser currentUser) {
        TradeRequest request = participantRequest(id, currentUser.id());
        requireSeller(request, currentUser.id());
        requireStatus(request, TradeStatus.PENDING);
        MarketplaceItem item = lockItemForAcceptance(request.getItemId());
        request.accept();
        item.changeStatus(ItemStatus.RESERVED);
        tradeRequestRepository.findByItemIdAndStatus(item.getId(), TradeStatus.PENDING).stream()
                .filter(other -> !other.getId().equals(request.getId()))
                .forEach(other -> {
                    other.reject();
                    notificationService.notify(
                            other.getRequesterId(),
                            currentUser.id(),
                            other.getId(),
                            null,
                            item.getId(),
                            MessageType.TRADE_REQUEST_REJECTED,
                            "你对《" + item.getTitle() + "》的交易请求已被拒绝（卖家已接受其他请求）。");
                });
        notificationService.notify(
                request.getRequesterId(),
                currentUser.id(),
                request.getId(),
                null,
                item.getId(),
                MessageType.TRADE_REQUEST_ACCEPTED,
                "卖家已接受你对《" + item.getTitle() + "》的交易请求。");
        return toResponse(request, item);
    }

    @Transactional
    public TradeRequestResponse reject(Long id, AuthenticatedUser currentUser) {
        TradeRequest request = participantRequest(id, currentUser.id());
        requireSeller(request, currentUser.id());
        requireStatus(request, TradeStatus.PENDING);
        request.reject();
        MarketplaceItem item = marketplaceItemRepository.findByIdForUpdate(request.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联物品不存在"));
        notificationService.notify(
                request.getRequesterId(),
                currentUser.id(),
                request.getId(),
                null,
                item.getId(),
                MessageType.TRADE_REQUEST_REJECTED,
                "卖家已拒绝你对《" + item.getTitle() + "》的交易请求。");
        return toResponse(request, item);
    }

    @Transactional
    public TradeRequestResponse cancel(Long id, AuthenticatedUser currentUser) {
        TradeRequest request = participantRequest(id, currentUser.id());
        if (!request.getRequesterId().equals(currentUser.id())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "只有请求发起人可以取消");
        }
        requireStatus(request, TradeStatus.PENDING);
        request.cancel();
        MarketplaceItem item = marketplaceItemRepository.findByIdForUpdate(request.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联物品不存在"));
        notificationService.notify(
                request.getSellerId(),
                currentUser.id(),
                request.getId(),
                null,
                item.getId(),
                MessageType.TRADE_REQUEST_CANCELLED,
                "买家已取消对《" + item.getTitle() + "》的交易请求。");
        return toResponse(request, item);
    }

    @Transactional
    public TradeRequestResponse confirmCompletion(Long id, AuthenticatedUser currentUser) {
        TradeRequest request = participantRequest(id, currentUser.id());
        if (request.getStatus() == TradeStatus.DISPUTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "交易处于争议中，等待管理员仲裁");
        }
        requireStatus(request, TradeStatus.ACCEPTED);
        MarketplaceItem item = marketplaceItemRepository.findByIdForUpdate(request.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联物品不存在"));
        requireTradableItem(item);
        boolean alreadyConfirmed = (request.getRequesterId().equals(currentUser.id())
                && request.getRequesterConfirmedAt() != null)
                || (request.getSellerId().equals(currentUser.id())
                && request.getSellerConfirmedAt() != null);
        if (alreadyConfirmed) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "你已经确认过该交易");
        }
        request.confirmBy(currentUser.id(), Instant.now());
        if (request.getRequesterConfirmedAt() != null && request.getSellerConfirmedAt() != null) {
            request.complete();
            item.changeStatus(ItemStatus.SOLD);
            String completedContent = "交易《" + item.getTitle() + "》已完成，双方可进行评价。";
            notificationService.notify(
                    request.getRequesterId(),
                    null,
                    request.getId(),
                    null,
                    item.getId(),
                    MessageType.TRADE_COMPLETED,
                    completedContent);
            notificationService.notify(
                    request.getSellerId(),
                    null,
                    request.getId(),
                    null,
                    item.getId(),
                    MessageType.TRADE_COMPLETED,
                    completedContent);
            return toResponse(request, item);
        }
        Long otherPartyId = request.getRequesterId().equals(currentUser.id())
                ? request.getSellerId()
                : request.getRequesterId();
        notificationService.notify(
                otherPartyId,
                currentUser.id(),
                request.getId(),
                null,
                item.getId(),
                MessageType.TRADE_CONFIRMATION_RECEIVED,
                "对方已确认完成交易《" + item.getTitle() + "》，请及时确认。");
        return toResponse(request, item);
    }

    private MarketplaceItem lockItemForAcceptance(Long itemId) {
        MarketplaceItem item = marketplaceItemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联物品不存在"));
        if (item.getStatus() != ItemStatus.ON_SALE) {
            throw new BusinessException(
                    ErrorCode.ITEM_UNAVAILABLE,
                    "物品已被其他交易占用或已下架，请刷新后重试");
        }
        requireTradableItem(item);
        return item;
    }
    private TradeRequest participantRequest(Long id, Long userId) {
        return tradeRequestRepository.findByIdAndParticipant(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "交易请求不存在或无权访问"));
    }

    private void requireSeller(TradeRequest request, Long userId) {
        if (!request.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "只有物品卖家可以执行此操作");
        }
    }

    private void requireStatus(TradeRequest request, TradeStatus expected) {
        if (request.getStatus() != expected) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "交易请求当前状态不允许此操作");
        }
    }

    private MarketplaceItem item(Long itemId) {
        return marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联物品不存在"));
    }

    private TradeRequestResponse toResponse(TradeRequest request) {
        return toResponse(request, marketplaceItemRepository.findById(request.getItemId()).orElse(null));
    }

    private TradeRequestResponse toResponse(TradeRequest request, MarketplaceItem item) {
        return TradeRequestResponse.from(request, item);
    }

    private void requireActiveUser(Long userId) {
        if (userAccountRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用");
        }
    }

    private void requireTradableItem(MarketplaceItem item) {
        if (item.getModerationStatus() != ItemModerationStatus.NORMAL) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "物品已被管理员屏蔽");
        }
        if (userAccountRepository.findByIdAndStatus(item.getSellerId(), UserStatus.ACTIVE).isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "卖家账户不可用");
        }
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
