package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class BoardDto {

    private String title;
    private String content;
    private String nickName;

    // Create
    //title, content
    @Getter
    public static class Request {
        @NotNull
        private Long boardId;
        private String title;
        private String content;
        private String nickName;
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
            Long boardId, String title, String content, String nickName
    ){
    }
}
