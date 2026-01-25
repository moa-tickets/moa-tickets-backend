package stack.moaticket.application.dto;

import lombok.*;

public abstract class ChattingDto {

    @Getter
    @Builder
    public static class Response {
        private String message;
        private String senderNickname;
    }

    @Getter
    public static class Request {
        private String message;
        private String senderNickname;
    }

}
