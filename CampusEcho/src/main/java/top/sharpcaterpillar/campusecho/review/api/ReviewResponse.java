package top.sharpcaterpillar.campusecho.review.api;

import java.time.Instant;
import top.sharpcaterpillar.campusecho.review.domain.Review;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;

public record ReviewResponse(
        Long id,
        Long tradeRequestId,
        Long reviewerId,
        String reviewerDisplayName,
        String reviewerAvatarUrl,
        Long revieweeId,
        int rating,
        String comment,
        Instant createdAt) {

    public static ReviewResponse from(Review review, UserAccount reviewer) {
        return new ReviewResponse(
                review.getId(),
                review.getTradeRequestId(),
                review.getReviewerId(),
                reviewer == null ? null : reviewer.getDisplayName(),
                reviewer == null ? null : reviewer.getAvatarUrl(),
                review.getRevieweeId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
