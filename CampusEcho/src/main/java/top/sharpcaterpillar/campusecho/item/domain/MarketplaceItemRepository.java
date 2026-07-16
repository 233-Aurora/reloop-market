package top.sharpcaterpillar.campusecho.item.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, Long> {

    Optional<MarketplaceItem> findByIdAndSellerId(Long id, Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from MarketplaceItem item where item.id = :id and item.sellerId = :sellerId")
    Optional<MarketplaceItem> findOwnedByIdForUpdate(@Param("id") Long id, @Param("sellerId") Long sellerId);

    Optional<MarketplaceItem> findByIdAndStatus(Long id, ItemStatus status);

    @Query("""
            select item from MarketplaceItem item
            where item.id = :id
              and item.status = :status
              and item.moderationStatus = :moderationStatus
              and exists (
                  select user.id from UserAccount user
                  where user.id = item.sellerId and user.status = :sellerStatus
              )
            """)
    Optional<MarketplaceItem> findPublicById(
            @Param("id") Long id,
            @Param("status") ItemStatus status,
            @Param("moderationStatus") ItemModerationStatus moderationStatus,
            @Param("sellerStatus") UserStatus sellerStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from MarketplaceItem item where item.id = :id")
    Optional<MarketplaceItem> findByIdForUpdate(@Param("id") Long id);

    Page<MarketplaceItem> findBySellerId(Long sellerId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    long countByStatus(ItemStatus status);

    @Query("""
            select item from MarketplaceItem item
            where item.status = :status
              and item.moderationStatus = :moderationStatus
              and exists (
                  select user.id from UserAccount user
                  where user.id = item.sellerId and user.status = :sellerStatus
              )
              and (:categoryId is null or item.categoryId = :categoryId)
              and (:keyword = ''
                   or lower(item.title) like lower(concat('%', :keyword, '%'))
                   or lower(item.description) like lower(concat('%', :keyword, '%')))
            """)
    Page<MarketplaceItem> searchPublicItems(
            @Param("status") ItemStatus status,
            @Param("moderationStatus") ItemModerationStatus moderationStatus,
            @Param("sellerStatus") UserStatus sellerStatus,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
