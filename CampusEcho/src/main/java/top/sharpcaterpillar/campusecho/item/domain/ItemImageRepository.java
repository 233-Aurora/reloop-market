package top.sharpcaterpillar.campusecho.item.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    List<ItemImage> findByItemIdOrderBySortOrderAscIdAsc(Long itemId);

    Optional<ItemImage> findByIdAndItemId(Long id, Long itemId);

    Optional<ItemImage> findFirstByItemIdOrderBySortOrderAscIdAsc(Long itemId);

    Optional<ItemImage> findByItemIdAndPrimaryTrue(Long itemId);

    long countByItemId(Long itemId);

    @Query("select coalesce(max(image.sortOrder), -1) from ItemImage image where image.itemId = :itemId")
    int findMaxSortOrder(@Param("itemId") Long itemId);

    @Query("""
            select image from ItemImage image
            where image.itemId in :itemIds
            order by image.itemId, image.sortOrder, image.id
            """)
    List<ItemImage> findAllByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
