package top.sharpcaterpillar.campusecho.category.application;

import java.util.HashSet;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.category.api.CategoryAdminResponse;
import top.sharpcaterpillar.campusecho.category.api.CategoryCreateRequest;
import top.sharpcaterpillar.campusecho.category.api.CategoryUpdateRequest;
import top.sharpcaterpillar.campusecho.category.domain.ItemCategory;
import top.sharpcaterpillar.campusecho.category.domain.ItemCategoryRepository;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.admin.application.AdminAccessService;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@Service
public class CategoryAdminService {

    private final ItemCategoryRepository itemCategoryRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final AdminAccessService adminAccessService;

    public CategoryAdminService(
            ItemCategoryRepository itemCategoryRepository,
            MarketplaceItemRepository marketplaceItemRepository,
            AdminAccessService adminAccessService) {
        this.itemCategoryRepository = itemCategoryRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.adminAccessService = adminAccessService;
    }

    @Transactional
    public CategoryAdminResponse create(AuthenticatedUser currentUser, CategoryCreateRequest request) {
        requireAdmin(currentUser);
        validateParent(request.parentId(), null);
        ItemCategory category = ItemCategory.create(normalizeName(request.name()), request.parentId(), request.sortOrder());
        return save(category);
    }

    @Transactional
    public CategoryAdminResponse update(Long id, AuthenticatedUser currentUser, CategoryUpdateRequest request) {
        requireAdmin(currentUser);
        ItemCategory category = findCategory(id);
        validateParent(request.parentId(), id);
        category.update(normalizeName(request.name()), request.parentId(), request.sortOrder());
        return save(category);
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        ItemCategory category = findCategory(id);
        if (itemCategoryRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "请先删除或移动子分类");
        }
        if (marketplaceItemRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "分类仍被物品使用，不能删除");
        }
        itemCategoryRepository.delete(category);
    }

    private CategoryAdminResponse save(ItemCategory category) {
        try {
            ItemCategory saved = itemCategoryRepository.saveAndFlush(category);
            return new CategoryAdminResponse(saved.getId(), saved.getName(), saved.getParentId(), saved.getSortOrder());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "同级分类名称已存在");
        }
    }

    private ItemCategory findCategory(Long id) {
        return itemCategoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在"));
    }

    private void validateParent(Long parentId, Long categoryId) {
        if (parentId == null) {
            return;
        }
        if (categoryId != null && categoryId.equals(parentId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "分类不能成为自己的父分类");
        }
        if (itemCategoryRepository.findById(parentId).isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "父分类不存在");
        }
        if (categoryId == null) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && visited.add(cursor)) {
            if (categoryId.equals(cursor)) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能将分类移动到自己的子分类下");
            }
            cursor = itemCategoryRepository.findById(cursor).map(ItemCategory::getParentId).orElse(null);
        }
    }

    private void requireAdmin(AuthenticatedUser currentUser) {
        adminAccessService.requireAdmin(currentUser);
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
