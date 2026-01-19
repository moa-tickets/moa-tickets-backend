package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.ReviewDto;
import stack.moaticket.application.service.ReviewService;
import stack.moaticket.domain.member.entity.Member;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping
    public ResponseEntity<ReviewDto.ReviewResponseDto> createReview(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ReviewDto.ReviewRequestDto request
    ) {
        return ResponseEntity.ok(reviewService.createReview(memberId, request));
    }

    // 상품별 리뷰 조회
    @GetMapping
    public ResponseEntity<List<ReviewDto.ReviewResponseDto>> getReviews(
            @RequestParam Long concertId
    ) {
        return ResponseEntity.ok(reviewService.getByConcertId(concertId));
    }
}
