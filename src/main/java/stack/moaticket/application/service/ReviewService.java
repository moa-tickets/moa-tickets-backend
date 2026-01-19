package stack.moaticket.application.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import stack.moaticket.application.dto.ReviewDto;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.concert.service.ConcertService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.review.entity.Review;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.review.repository.ReviewRepository;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewService {
    @Value("${app.ai.url}")
    private String AI_SERVER_URL;

    private final Validator validator;

    private final MemberService memberService;

    private final ReviewRepository reviewRepository;
    private final ConcertService concertService;

    @Transactional
    public ReviewDto.ReviewResponseDto createReview(Long memberId, ReviewDto.ReviewRequestDto request) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        validateCreateRequest(request);

        Review review = Review.builder()
                .member(member)
                .concert(concertService.getConcertById(request.getConcertId()))
                .score(request.getScore())
                .content(request.getContent())
                .build();

        Review saved = reviewRepository.save(review);

//        ai 서버의 api랑 통신(HTTP, RestAPI)을 하면된다
        RestTemplate restTemplate = new RestTemplate();
        String url = AI_SERVER_URL + "/api/reviews";
        restTemplate.postForEntity(
                url,
                new ReviewDto.SpringReviewItemDto(
                        review.getId(),
                        review.getContent(),
                        member.getId(),
                        review.getConcert().getId()),
                ReviewDto.SpringReviewItemDto.class);

        return toResponse(saved);
    }



    @Transactional(readOnly = true)
    public List<ReviewDto.ReviewResponseDto> getByConcertId(Long concertId) {
        validateProductId(concertId);

        Concert concert = concertService.getConcertById(concertId);

        List<Review> reviews = reviewRepository.findAllByConcert(concert);

        List<ReviewDto.ReviewResponseDto> responses = new ArrayList<>();
        for (Review review : reviews) {
            responses.add(toResponse(review));
        }
        return responses;
    }

    private ReviewDto.ReviewResponseDto toResponse(Review review) {
        return new ReviewDto.ReviewResponseDto(
                review.getId(),
                review.getMember().getNickname(),
                review.getConcert().getName(),
                review.getScore(),
                review.getContent()
        );
    }

    private void validateCreateRequest(ReviewDto.ReviewRequestDto request) {
        if (request == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        validateProductId(request.getConcertId());

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
