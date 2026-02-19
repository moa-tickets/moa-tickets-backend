package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
public class CommentDto {

    @Getter
    public static class Request {
        @NotNull
        private String content;
    }

    @Getter
    @Builder
    public static class CommentResponse {
        private Long commentId;
        private String content;
        private String nickName;
        private LocalDateTime createdAt;
    }
    public record CommentFixRequest(
            @NotNull String content
    ) {

    }
}