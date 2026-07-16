package top.sharpcaterpillar.campusecho.review.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByTradeRequestIdAndReviewerId(Long tradeRequestId, Long reviewerId);

    List<Review> findByTradeRequestIdOrderByCreatedAtAscIdAsc(Long tradeRequestId);

    Page<Review> findByRevieweeId(Long revieweeId, Pageable pageable);

    @Query("select avg(review.rating) from Review review where review.revieweeId = :revieweeId")
    Double averageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);
}
