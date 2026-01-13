package stack.moaticket.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;



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
        private final String memberNickname;
        private final String concertName;
        private final double score;
        private final String content;

        public ReviewResponseDto(Long reviewId, String memberNickname, String concertName, double score, String content) {
            this.reviewId = reviewId;
            this.memberNickname = memberNickname;
            this.concertName = concertName;
            this.score = score;
            this.content = content;
        }
    }
}
