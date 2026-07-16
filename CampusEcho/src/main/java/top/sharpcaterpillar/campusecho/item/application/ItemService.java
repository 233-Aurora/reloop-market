package top.sharpcaterpillar.campusecho.item.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.category.domain.ItemCategory;
import top.sharpcaterpillar.campusecho.category.domain.ItemCategoryRepository;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.item.api.ItemCreateRequest;
import top.sharpcaterpillar.campusecho.item.api.ItemResponse;
import top.sharpcaterpillar.campusecho.item.api.ItemImageResponse;
import top.sharpcaterpillar.campusecho.item.api.ItemUpdateRequest;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemImageRepository;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class ItemService {

    private final MarketplaceItemRepository marketplaceItemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final ItemImageRepository itemImageRepository;

    public ItemService(
            MarketplaceItemRepository marketplaceItemRepository,
            ItemCategoryRepository itemCategoryRepository,
            UserAccountRepository userAccountRepository,
            ItemImageRepository itemImageRepository) {
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.itemCategoryRepository = itemCategoryRepository;
        this.userAccountRepository = userAccountRepository;
        this.itemImageRepository = itemImageRepository;
    }

    @Transactional
    public ItemResponse create(AuthenticatedUser currentUser, ItemCreateRequest request) {
        requireActiveUser(currentUser.id());
        requireCategory(request.categoryId());
        MarketplaceItem item = MarketplaceItem.publish(
                currentUser.id(),
                request.categoryId(),
                request.title().trim(),
                request.description().trim(),
                request.price(),
                request.condition(),
                normalizeOptional(request.locationText()));
        return response(marketplaceItemRepository.save(item), categoryName(request.categoryId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> listPublic(Long categoryId, String keyword, int page, int size) {
        String normalizedKeyword = normalizeOptional(keyword);
        Page<MarketplaceItem> items = marketplaceItemRepository.searchPublicItems(
                ItemStatus.ON_SALE,
                ItemModerationStatus.NORMAL,
                UserStatus.ACTIVE,
                categoryId,
                normalizedKeyword == null ? "" : normalizedKeyword,
                pageable(page, size));
        return toPageResponse(items);
    }

    @Transactional(readOnly = true)
    public ItemResponse getPublicDetail(Long id) {
        MarketplaceItem item = marketplaceItemRepository.findPublicById(
                        id, ItemStatus.ON_SALE, ItemModerationStatus.NORMAL, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "物品不存在"));
        return response(item, categoryName(item.getCategoryId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> getMine(AuthenticatedUser currentUser, int page, int size) {
        requireActiveUser(currentUser.id());
        return toPageResponse(marketplaceItemRepository.findBySellerId(currentUser.id(), pageable(page, size)));
    }

    @Transactional
    public ItemResponse update(Long id, AuthenticatedUser currentUser, ItemUpdateRequest request) {
        requireCategory(request.categoryId());
        MarketplaceItem item = ownedItem(id, currentUser.id());
        requireModerationNormal(item);
        if (item.getStatus() == ItemStatus.SOLD || item.getStatus() == ItemStatus.RESERVED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前物品状态不允许编辑");
        }
        item.update(
                request.categoryId(),
                request.title().trim(),
                request.description().trim(),
                request.price(),
                request.condition(),
                normalizeOptional(request.locationText()));
        return response(item, categoryName(item.getCategoryId()));
    }

    @Transactional
    public ItemResponse offShelf(Long id, AuthenticatedUser currentUser) {
        MarketplaceItem item = ownedItem(id, currentUser.id());
        requireModerationNormal(item);
        if (item.getStatus() != ItemStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅在售物品可以下架");
        }
        item.changeStatus(ItemStatus.OFF_SHELF);
        return response(item, categoryName(item.getCategoryId()));
    }

    @Transactional
    public ItemResponse publish(Long id, AuthenticatedUser currentUser) {
        MarketplaceItem item = ownedItem(id, currentUser.id());
        requireModerationNormal(item);
        if (item.getStatus() != ItemStatus.OFF_SHELF && item.getStatus() != ItemStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前物品状态不允许上架");
        }
        item.changeStatus(ItemStatus.ON_SALE);
        return response(item, categoryName(item.getCategoryId()));
    }

    private PageResponse<ItemResponse> toPageResponse(Page<MarketplaceItem> items) {
        Map<Long, String> categoryNames = categoryNames(items.getContent());
        Map<Long, List<ItemImageResponse>> images = imageResponses(items.getContent());
        return PageResponse.from(items.map(item -> ItemResponse.from(
                item, categoryNames.get(item.getCategoryId()), images.getOrDefault(item.getId(), List.of()))));
    }

    private ItemResponse response(MarketplaceItem item, String categoryName) {
        return ItemResponse.from(item, categoryName, imageResponses(List.of(item)).getOrDefault(item.getId(), List.of()));
    }

    private Map<Long, List<ItemImageResponse>> imageResponses(Collection<MarketplaceItem> items) {
        List<Long> itemIds = items.stream().map(MarketplaceItem::getId).toList();
        Map<Long, List<ItemImageResponse>> result = new HashMap<>();
        if (itemIds.isEmpty()) {
            return result;
        }
        itemImageRepository.findAllByItemIds(itemIds).forEach(image ->
                result.computeIfAbsent(image.getItemId(), ignored -> new java.util.ArrayList<>())
                        .add(ItemImageResponse.from(image)));
        return result;
    }

    private Map<Long, String> categoryNames(Collection<MarketplaceItem> items) {
        Map<Long, String> categoryNames = new HashMap<>();
        itemCategoryRepository.findAllById(items.stream().map(MarketplaceItem::getCategoryId).distinct().toList())
                .forEach(category -> categoryNames.put(category.getId(), category.getName()));
        return categoryNames;
    }

    private MarketplaceItem ownedItem(Long id, Long sellerId) {
        return marketplaceItemRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "物品不存在或无权操作"));
    }

    private void requireModerationNormal(MarketplaceItem item) {
        if (item.getModerationStatus() != ItemModerationStatus.NORMAL) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "物品已被管理员屏蔽，当前操作不可用");
        }
    }

    private void requireActiveUser(Long userId) {
        if (userAccountRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用");
        }
    }

    private void requireCategory(Long categoryId) {
        if (!itemCategoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
        }
    }

    private String categoryName(Long categoryId) {
        return itemCategoryRepository.findById(categoryId).map(ItemCategory::getName).orElse(null);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
