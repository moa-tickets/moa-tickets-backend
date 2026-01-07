package stack.moaticket.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.ReviewDto;
import stack.moaticket.application.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // 리뷰 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto.ReviewResponseDto createReview(
            @RequestBody ReviewDto.ReviewRequestDto request
    ) {
        return reviewService.createReview(request);
    }

    // 상품별 리뷰 조회
    @GetMapping
    public List<ReviewDto.ReviewResponseDto> getReviews(
            @RequestParam Long productId
    ) {
        return reviewService.getReviewsByProductId(productId);
    }
}
