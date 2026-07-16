package top.sharpcaterpillar.campusecho.review.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
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
import top.sharpcaterpillar.campusecho.review.application.ReviewService;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@Validated
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/trade-requests/{tradeRequestId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @PathVariable Long tradeRequestId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = reviewService.create(tradeRequestId, currentUser, request);
        return ResponseEntity.created(URI.create("/api/v1/trade-requests/" + tradeRequestId + "/reviews"))
                .body(ApiResponse.success(response));
    }

    @GetMapping("/trade-requests/{tradeRequestId}/reviews")
    public ApiResponse<List<ReviewResponse>> listForTrade(
            @PathVariable Long tradeRequestId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(reviewService.listForTrade(tradeRequestId, currentUser));
    }

    @GetMapping("/users/{userId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> listReceived(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(reviewService.listReceived(userId, page, size));
    }
}
