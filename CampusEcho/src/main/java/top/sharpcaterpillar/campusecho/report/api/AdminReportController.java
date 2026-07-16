package top.sharpcaterpillar.campusecho.report.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.report.application.ReportService;
import top.sharpcaterpillar.campusecho.report.domain.ReportReason;
import top.sharpcaterpillar.campusecho.report.domain.ReportStatus;
import top.sharpcaterpillar.campusecho.report.domain.ReportTargetType;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<ReportResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportReason reason,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(reportService.listForAdmin(
                currentUser, targetType, reason, status, page, size));
    }

    @GetMapping("/reports/{reportId}")
    public ApiResponse<ReportResponse> detail(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(reportService.detailForAdmin(reportId, currentUser));
    }

    @PostMapping("/reports/{reportId}/resolve")
    public ApiResponse<ReportResponse> resolve(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ReportResolutionRequest request) {
        return ApiResponse.success(reportService.resolve(
                reportId, request.action(), request.resolutionNote(), currentUser));
    }

    @PostMapping("/reports/{reportId}/reject")
    public ApiResponse<ReportResponse> reject(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ReportRejectionRequest request) {
        return ApiResponse.success(reportService.reject(reportId, request.resolutionNote(), currentUser));
    }

    @PostMapping("/items/{itemId}/restore")
    public ApiResponse<ItemModerationResponse> restoreItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ItemRestoreRequest request) {
        return ApiResponse.success(reportService.restoreItem(itemId, request.reason(), currentUser));
    }
}
