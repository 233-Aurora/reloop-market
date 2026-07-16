package top.sharpcaterpillar.campusecho.trade.api;

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
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.trade.application.TradeRequestService;

@RestController
@Validated
@RequestMapping("/api/v1/trade-requests")
public class TradeRequestController {

    private final TradeRequestService tradeRequestService;

    public TradeRequestController(TradeRequestService tradeRequestService) {
        this.tradeRequestService = tradeRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TradeRequestResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TradeRequestCreateRequest request) {
        TradeRequestResponse response = tradeRequestService.create(currentUser, request);
        return ResponseEntity.created(URI.create("/api/v1/trade-requests/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<PageResponse<TradeRequestResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(tradeRequestService.listMine(currentUser, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TradeRequestResponse> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(tradeRequestService.detail(id, currentUser));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<TradeRequestResponse> accept(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(tradeRequestService.accept(id, currentUser));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<TradeRequestResponse> reject(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(tradeRequestService.reject(id, currentUser));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<TradeRequestResponse> cancel(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(tradeRequestService.cancel(id, currentUser));
    }

    @PostMapping("/{id}/confirm-completion")
    public ApiResponse<TradeRequestResponse> confirmCompletion(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(tradeRequestService.confirmCompletion(id, currentUser));
    }
}
