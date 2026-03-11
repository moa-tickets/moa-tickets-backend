package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@NoArgsConstructor
public abstract class BoardDto {
    // Create
    //title, content
    @Getter
    public static class BoardPostRequest {
        @NotNull private String title;
        @NotNull private String content;
    }

    @Getter
    @Builder
    public static class BoardResponse {
        private Long boardId;
        private String title;
        private String content;
        private String nickName;
        private long likeCount; //해당 게시글 좋아요 수
        private LocalDateTime createdAt;
    }

    public record BoardFixRequest(
            @NotNull String title,
            @NotNull String content
    ){
    }
}
