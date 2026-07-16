package top.sharpcaterpillar.campusecho.category.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {

    List<ItemCategory> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByParentId(Long parentId);
}
