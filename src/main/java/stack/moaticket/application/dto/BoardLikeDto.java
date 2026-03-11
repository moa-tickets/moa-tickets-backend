package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
public class BoardLikeDto {

    // POST/DELETE 응답
    @Getter
    @Builder
    public static class BoardLikeActionResponse{
        private Long boardId;
        private boolean myLiked;  // 현재 내 좋아요 상태
        private long likeCount;   // 해당 게시글 좋아요 수
    }

    @Builder
    @Getter
    public static class BoardLikeResponse {
        private Long boardLikeId;
        private Long boardId;               // 어떤 게시글 좋아요인지
        private LocalDateTime createdAt; // 좋아요 누른 시각
        private long likeCount;
    }
}
