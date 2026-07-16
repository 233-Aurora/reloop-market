package top.sharpcaterpillar.campusecho.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "reports")
public class ModerationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private ReportTargetType targetType;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "reported_user_id")
    private Long reportedUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportReason reason;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ReportAction action;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private Instant handledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ModerationReport() {
    }

    private ModerationReport(
            Long reporterId,
            ReportTargetType targetType,
            Long itemId,
            Long reportedUserId,
            ReportReason reason,
            String description) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.itemId = itemId;
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.description = description;
        this.status = ReportStatus.PENDING;
    }

    public static ModerationReport forItem(
            Long reporterId, Long itemId, ReportReason reason, String description) {
        return new ModerationReport(reporterId, ReportTargetType.ITEM, itemId, null, reason, description);
    }

    public static ModerationReport forUser(
            Long reporterId, Long reportedUserId, ReportReason reason, String description) {
        return new ModerationReport(reporterId, ReportTargetType.USER, null, reportedUserId, reason, description);
    }

    public void resolve(ReportAction action, String resolutionNote, Long handledBy, Instant handledAt) {
        this.status = ReportStatus.RESOLVED;
        this.action = action;
        this.resolutionNote = resolutionNote;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
    }

    public void reject(String resolutionNote, Long handledBy, Instant handledAt) {
        this.status = ReportStatus.REJECTED;
        this.action = ReportAction.NONE;
        this.resolutionNote = resolutionNote;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getReportedUserId() {
        return reportedUserId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public ReportAction getAction() {
        return action;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public Instant getHandledAt() {
        return handledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
