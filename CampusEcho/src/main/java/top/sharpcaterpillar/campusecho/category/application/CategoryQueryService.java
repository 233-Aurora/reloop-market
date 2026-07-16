package top.sharpcaterpillar.campusecho.category.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.category.api.CategoryTreeResponse;
import top.sharpcaterpillar.campusecho.category.domain.ItemCategory;
import top.sharpcaterpillar.campusecho.category.domain.ItemCategoryRepository;

@Service
public class CategoryQueryService {

    private final ItemCategoryRepository itemCategoryRepository;

    public CategoryQueryService(ItemCategoryRepository itemCategoryRepository) {
        this.itemCategoryRepository = itemCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getTree() {
        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        for (ItemCategory category : itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc()) {
            nodes.put(category.getId(), new CategoryNode(category.getId(), category.getName(), category.getParentId()));
        }

        List<CategoryNode> roots = new ArrayList<>();
        for (CategoryNode node : nodes.values()) {
            CategoryNode parent = node.parentId == null ? null : nodes.get(node.parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }
        return roots.stream().map(CategoryNode::toResponse).toList();
    }

    private static final class CategoryNode {
        private final Long id;
        private final String name;
        private final Long parentId;
        private final List<CategoryNode> children = new ArrayList<>();

        private CategoryNode(Long id, String name, Long parentId) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
        }

        private CategoryTreeResponse toResponse() {
            return new CategoryTreeResponse(id, name, children.stream().map(CategoryNode::toResponse).toList());
        }
    }
}
