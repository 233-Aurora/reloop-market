package top.sharpcaterpillar.campusecho.trade.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface TradeRequestRepository extends JpaRepository<TradeRequest, Long> {

    boolean existsByItemIdAndRequesterIdAndStatusIn(Long itemId, Long requesterId, List<TradeStatus> statuses);

    List<TradeRequest> findByItemIdAndStatus(Long itemId, TradeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request from TradeRequest request
            where request.itemId = :itemId and request.status in :statuses
            order by request.id
            """)
    List<TradeRequest> findByItemIdAndStatusInForUpdate(
            @Param("itemId") Long itemId,
            @Param("statuses") List<TradeStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request from TradeRequest request
            where (request.requesterId = :userId or request.sellerId = :userId)
              and request.status in :statuses
            order by request.id
            """)
    List<TradeRequest> findByParticipantAndStatusInForUpdate(
            @Param("userId") Long userId,
            @Param("statuses") List<TradeStatus> statuses);

    long countByStatus(TradeStatus status);

    @Query("""
            select request from TradeRequest request
            where request.requesterId = :userId or request.sellerId = :userId
            order by request.createdAt desc
            """)
    Page<TradeRequest> findMine(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select request from TradeRequest request
            where request.id = :id and (request.requesterId = :userId or request.sellerId = :userId)
            """)
    Optional<TradeRequest> findByIdAndParticipant(@Param("id") Long id, @Param("userId") Long userId);
}
