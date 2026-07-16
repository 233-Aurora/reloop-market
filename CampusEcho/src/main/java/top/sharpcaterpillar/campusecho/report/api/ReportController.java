package top.sharpcaterpillar.campusecho.report.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
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
import top.sharpcaterpillar.campusecho.report.domain.ReportStatus;
import top.sharpcaterpillar.campusecho.report.domain.ReportTargetType;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@Validated
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ReportCreateRequest request) {
        ReportResponse response = reportService.create(currentUser, request);
        return ResponseEntity.created(URI.create("/api/v1/reports/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ReportResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(reportService.listMine(currentUser, targetType, status, page, size));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<ReportResponse> detail(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(reportService.detailMine(reportId, currentUser));
    }
}
