package stack.moaticket.domain.concert_review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.concert_review.entity.ConcertReview;

@Repository
public interface ConcertReviewRepository extends JpaRepository<ConcertReview, Long> {
}
