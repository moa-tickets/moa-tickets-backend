package stack.moaticket.application.service;


import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.ReviewDto;
import stack.moaticket.domain.review.entity.Review;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.review.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewDto.ReviewResponseDto createReview(ReviewDto.ReviewRequestDto request) {
        validateCreateRequest(request);

        Review review = Review.builder()
                .productId(request.getProductId())
                .score(request.getScore())
                .content(request.getContent())
                .build();

        Review saved = reviewRepository.save(review);

        return toResponse(saved);
    }

    public List<ReviewDto.ReviewResponseDto> getReviewsByProductId(Long productId) {
        validateProductId(productId);

        List<Review> reviews = reviewRepository.findByProductId(productId);

        List<ReviewDto.ReviewResponseDto> responses = new ArrayList<>();
        for (Review review : reviews) {
            responses.add(toResponse(review));
        }
        return responses;
    }

    private ReviewDto.ReviewResponseDto toResponse(Review review) {
        return new ReviewDto.ReviewResponseDto(
                review.getId(),
                review.getProductId(),
                review.getScore(),
                review.getContent()
        );
    }

    private void validateCreateRequest(ReviewDto.ReviewRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("요청이 비어있습니다.");
        }

        validateProductId(request.getProductId());

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("리뷰 내용은 필수입니다.");
        }

        if (request.getScore() < 1 || request.getScore() > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }
    }

    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }
        if (productId <= 0) {
            throw new IllegalArgumentException("productId는 1 이상이어야 합니다.");
        }
    }
}
