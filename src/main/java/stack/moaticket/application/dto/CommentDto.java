package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CommentDto {

    @Getter
    public static class Request {
        private String content;
    }

    @Getter
    public static class commentResponse {
        private Long commentId;
        private String nickName;
        private String content;
    }

    public record CommentFixRequest (
            Long commentId, String nickName, String content
    ){
    }
}
