package top.sharpcaterpillar.campusecho.dispute.api;

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
import top.sharpcaterpillar.campusecho.dispute.application.DisputeService;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeReason;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeStatus;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@Validated
@RequestMapping("/api/v1/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DisputeResponse>> open(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody DisputeCreateRequest request) {
        DisputeResponse response = disputeService.open(currentUser, request);
        return ResponseEntity.created(URI.create("/api/v1/disputes/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<DisputeResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(required = false) DisputeReason reason,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(disputeService.listMine(currentUser, status, reason, page, size));
    }

    @GetMapping("/{disputeId}")
    public ApiResponse<DisputeResponse> detail(
            @PathVariable Long disputeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(disputeService.detailMine(disputeId, currentUser));
    }
}
