package top.sharpcaterpillar.campusecho.review.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.review.api.ReviewCreateRequest;
import top.sharpcaterpillar.campusecho.review.api.ReviewResponse;
import top.sharpcaterpillar.campusecho.review.domain.Review;
import top.sharpcaterpillar.campusecho.review.domain.ReviewRepository;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequest;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequestRepository;
import top.sharpcaterpillar.campusecho.trade.domain.TradeStatus;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TradeRequestRepository tradeRequestRepository;
    private final UserAccountRepository userAccountRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            TradeRequestRepository tradeRequestRepository,
            UserAccountRepository userAccountRepository) {
        this.reviewRepository = reviewRepository;
        this.tradeRequestRepository = tradeRequestRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public ReviewResponse create(Long tradeRequestId, AuthenticatedUser currentUser, ReviewCreateRequest request) {
        UserAccount reviewer = activeUser(currentUser.id());
        TradeRequest tradeRequest = participantTrade(tradeRequestId, currentUser.id());
        if (tradeRequest.getStatus() != TradeStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅已完成的交易可以评价");
        }

        Long revieweeId = tradeRequest.getRequesterId().equals(currentUser.id())
                ? tradeRequest.getSellerId()
                : tradeRequest.getRequesterId();
        UserAccount reviewee = userAccountRepository.findByIdForUpdate(revieweeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "被评价用户不存在"));
        if (reviewRepository.existsByTradeRequestIdAndReviewerId(tradeRequestId, currentUser.id())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该交易已经评价过");
        }

        Review review = Review.create(
                tradeRequestId,
                currentUser.id(),
                revieweeId,
                request.rating(),
                normalizeOptional(request.comment()));
        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该交易已经评价过");
        }

        Double average = reviewRepository.averageRatingByRevieweeId(revieweeId);
        reviewee.updateReputationScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        userAccountRepository.save(reviewee);
        return ReviewResponse.from(review, reviewer);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForTrade(Long tradeRequestId, AuthenticatedUser currentUser) {
        activeUser(currentUser.id());
        participantTrade(tradeRequestId, currentUser.id());
        return toResponses(reviewRepository.findByTradeRequestIdOrderByCreatedAtAscIdAsc(tradeRequestId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listReceived(Long userId, int page, int size) {
        if (userAccountRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        Page<Review> reviews = reviewRepository.findByRevieweeId(userId, pageable(page, size));
        Map<Long, UserAccount> reviewers = reviewers(reviews.getContent());
        return PageResponse.from(reviews.map(review -> ReviewResponse.from(review, reviewers.get(review.getReviewerId()))));
    }

    private List<ReviewResponse> toResponses(List<Review> reviews) {
        Map<Long, UserAccount> reviewers = reviewers(reviews);
        return reviews.stream()
                .map(review -> ReviewResponse.from(review, reviewers.get(review.getReviewerId())))
                .toList();
    }

    private Map<Long, UserAccount> reviewers(List<Review> reviews) {
        Map<Long, UserAccount> result = new HashMap<>();
        userAccountRepository.findAllById(reviews.stream().map(Review::getReviewerId).distinct().toList())
                .forEach(user -> result.put(user.getId(), user));
        return result;
    }

    private UserAccount activeUser(Long userId) {
        return userAccountRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用"));
    }

    private TradeRequest participantTrade(Long tradeRequestId, Long userId) {
        return tradeRequestRepository.findByIdAndParticipant(tradeRequestId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "交易请求不存在或无权访问"));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
