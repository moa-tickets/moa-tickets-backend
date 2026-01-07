package stack.moaticket.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;



public abstract class ReviewDto {
    @NoArgsConstructor
    @Getter
    //    =============Request==============
    //    CreateRequest
    public static class ReviewRequestDto {
        private Long productId;
        private double score;
        private String content;
    }

    @Getter
    //    =============Response==============
    public static class ReviewResponseDto {

        private final Long reviewId;
        private final Long productId;
        private final double score;
        private final String content;

        public ReviewResponseDto(Long reviewId, Long productId, double score, String content) {
            this.reviewId = reviewId;
            this.productId = productId;
            this.score = score;
            this.content = content;
        }
    }
}
