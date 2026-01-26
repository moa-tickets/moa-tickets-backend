package stack.moaticket.application.dto;

import lombok.*;
import stack.moaticket.domain.chat_message.entity.ChatMessage;

public abstract class ChattingDto {

    @Getter
    @Builder
    public static class Response {
        private String message;
        private String senderNickname;
        private Long chatMessageId;

        public static Response toResponse(ChatMessage chatMessage) {
            return Response.builder()
                    .message(chatMessage.getContent())
                    .senderNickname(chatMessage.getNickname())
                    .chatMessageId(chatMessage.getId())
                    .build();
        }
    }

    @Getter
    public static class Request {
        private String message;
    }

}
