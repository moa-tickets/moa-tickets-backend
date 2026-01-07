package stack.moaticket.application.service;


import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.ReviewDto;
import stack.moaticket.domain.review.entity.Review;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.review.repository.ReviewRepository;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

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
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        validateProductId(request.getProductId());

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        if (request.getScore() < 1 || request.getScore() > 5) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
    }

    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (productId <= 0) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
    }
}
