package stack.moaticket.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


public abstract class ReviewDto {
    @NoArgsConstructor
    @Getter
    //    =============Request==============
    //    CreateRequest
    public static class ReviewRequestDto {
        private Long concertId;
        private double score;
        private String content;
    }

    @Getter
    //    =============Response==============
    public static class ReviewResponseDto {

        private final Long reviewId;
        private final Long concertId;
        private final String memberNickname;
        private final String concertName;
        private final double score;
        private final String content;

        public ReviewResponseDto(Long reviewId, Long concertId, String memberNickname, String concertName, double score, String content) {
            this.reviewId = reviewId;
            this.concertId = concertId;
            this.memberNickname = memberNickname;
            this.concertName = concertName;
            this.score = score;
            this.content = content;
        }


    }
    @Getter
    public static class SpringReviewItemDto {
        private final List<Review> reviews;

        public SpringReviewItemDto(Long reviewId, String content, Long userId, Long concertId) {
            List<Review> reviews = new ArrayList<>();
            Review review = new Review(reviewId, content, userId, concertId);

            reviews.add(review);
            this.reviews = reviews;
        }

        private record Review (Long reviewId, String content, Long userId, Long concertId) {}

    }
}
