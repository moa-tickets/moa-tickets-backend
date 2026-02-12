package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CommentDto {

    @Getter
    public static class Request {
        @NotNull
        private String content;
    }

    @Getter
    public static class CommentResponse {
        private Long commentId;
        private String nickName;
        private String content;
    }

    public record CommentFixRequest(
            @NotNull String content
    ) {
    }
}