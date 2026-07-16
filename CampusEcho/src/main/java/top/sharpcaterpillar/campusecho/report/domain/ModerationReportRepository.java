package top.sharpcaterpillar.campusecho.report.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModerationReportRepository extends JpaRepository<ModerationReport, Long> {

    boolean existsByReporterIdAndItemIdAndStatus(Long reporterId, Long itemId, ReportStatus status);

    boolean existsByReporterIdAndReportedUserIdAndStatus(Long reporterId, Long reportedUserId, ReportStatus status);

    long countByStatus(ReportStatus status);

    Optional<ModerationReport> findByIdAndReporterId(Long id, Long reporterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from ModerationReport report where report.id = :id")
    Optional<ModerationReport> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select report from ModerationReport report
            where report.reporterId = :reporterId
              and (:targetType is null or report.targetType = :targetType)
              and (:status is null or report.status = :status)
            """)
    Page<ModerationReport> searchMine(
            @Param("reporterId") Long reporterId,
            @Param("targetType") ReportTargetType targetType,
            @Param("status") ReportStatus status,
            Pageable pageable);

    @Query("""
            select report from ModerationReport report
            where (:targetType is null or report.targetType = :targetType)
              and (:reason is null or report.reason = :reason)
              and (:status is null or report.status = :status)
            """)
    Page<ModerationReport> searchForAdmin(
            @Param("targetType") ReportTargetType targetType,
            @Param("reason") ReportReason reason,
            @Param("status") ReportStatus status,
            Pageable pageable);
}
