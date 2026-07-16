package top.sharpcaterpillar.campusecho.dispute.api;

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
import top.sharpcaterpillar.campusecho.dispute.application.DisputeService;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeReason;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeStatus;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@Validated
@RequestMapping("/api/v1/admin/disputes")
public class AdminDisputeController {

    private final DisputeService disputeService;

    public AdminDisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DisputeResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(required = false) DisputeReason reason,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(disputeService.listForAdmin(currentUser, status, reason, page, size));
    }

    @GetMapping("/{disputeId}")
    public ApiResponse<DisputeResponse> detail(
            @PathVariable Long disputeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(disputeService.detailForAdmin(disputeId, currentUser));
    }

    @PostMapping("/{disputeId}/resolve")
    public ApiResponse<DisputeResponse> resolve(
            @PathVariable Long disputeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody DisputeResolutionRequest request) {
        return ApiResponse.success(disputeService.resolve(
                disputeId, request.resolution(), request.resolutionNote(), currentUser));
    }

    @PostMapping("/{disputeId}/reject")
    public ApiResponse<DisputeResponse> reject(
            @PathVariable Long disputeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody DisputeRejectionRequest request) {
        return ApiResponse.success(disputeService.reject(disputeId, request.resolutionNote(), currentUser));
    }
}
