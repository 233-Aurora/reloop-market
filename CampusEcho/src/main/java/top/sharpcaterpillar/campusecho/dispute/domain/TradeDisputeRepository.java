package top.sharpcaterpillar.campusecho.dispute.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeDisputeRepository extends JpaRepository<TradeDispute, Long> {

    boolean existsByTradeRequestIdAndStatus(Long tradeRequestId, DisputeStatus status);

    long countByStatus(DisputeStatus status);

    Optional<TradeDispute> findByIdAndOpenerId(Long id, Long openerId);

    @Query("""
            select dispute from TradeDispute dispute
            where dispute.id = :id
              and (dispute.openerId = :userId or dispute.respondentId = :userId)
            """)
    Optional<TradeDispute> findByIdAndParticipant(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select dispute from TradeDispute dispute where dispute.id = :id")
    Optional<TradeDispute> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select dispute from TradeDispute dispute
            where dispute.tradeRequestId in :tradeRequestIds and dispute.status = :status
            order by dispute.id
            """)
    List<TradeDispute> findByTradeRequestIdInAndStatusForUpdate(
            @Param("tradeRequestIds") List<Long> tradeRequestIds,
            @Param("status") DisputeStatus status);

    @Query("""
            select dispute from TradeDispute dispute
            where (dispute.openerId = :userId or dispute.respondentId = :userId)
              and (:status is null or dispute.status = :status)
              and (:reason is null or dispute.reason = :reason)
            """)
    Page<TradeDispute> searchMine(
            @Param("userId") Long userId,
            @Param("status") DisputeStatus status,
            @Param("reason") DisputeReason reason,
            Pageable pageable);

    @Query("""
            select dispute from TradeDispute dispute
            where (:status is null or dispute.status = :status)
              and (:reason is null or dispute.reason = :reason)
            """)
    Page<TradeDispute> searchForAdmin(
            @Param("status") DisputeStatus status,
            @Param("reason") DisputeReason reason,
            Pageable pageable);
}
