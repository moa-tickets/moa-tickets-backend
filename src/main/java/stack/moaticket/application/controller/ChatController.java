package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.domain.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;


    @MessageMapping("/send/{roomId}")
    public void sendMessage(
            @Header("simpSessionAttributes") Map<String, Object> sessionAttributes,
            ChattingDto request,
            @DestinationVariable String roomId) {
        String userNickname = (String) sessionAttributes.get("userNickname");
        if (userNickname == null) {
            log.error("세션에서 userNickname을 찾을 수 없습니다. roomId: {}", roomId);
            return;
        }
        ChattingDto chattingDto = ChattingDto.builder()
                .message(request.getMessage())
                .senderNickname(userNickname)
                .build();

        log.info("방 아이디: {}, 메세지: {}, nick: {}", roomId, chattingDto.getMessage(), userNickname);
        messagingTemplate.convertAndSend("/sub/" + roomId + "/messages", chattingDto);
    }
}
