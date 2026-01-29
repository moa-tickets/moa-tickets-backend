package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public abstract class BoardDto {
    // Create
    //title, content
    @Getter
    public static class Request {
        private String title;
        private String content;
    }

    @Getter
    @Builder
    public static class BoardResponse {
        private Long boardId;
        private String title;
        private String content;
        private String nickName;
    }

    public record BoardFixRequest(
            String title, String content, String nickName
    ){
    }
}
