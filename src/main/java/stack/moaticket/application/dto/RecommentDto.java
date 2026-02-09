package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
public class RecommentDto {

    @Getter
    public static class Request {
        @NotNull
        private String content;
    }

    @Getter
    @SuperBuilder
    public static class RecommentResponse {
        private Long recommentId;
        private String nickName;
        private String content;
    }

    public record RecommentFixRequest(
            @NotNull String content
    ) {
    }
}
