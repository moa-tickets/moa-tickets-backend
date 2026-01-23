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
import stack.moaticket.application.service.ChattingService;
import stack.moaticket.domain.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChattingService chattingService;


    @MessageMapping("/send/{playbackId}")
    public void sendMessage(
            @Header("simpSessionAttributes") Map<String, Object> sessionAttributes,
            ChattingDto request,
            @DestinationVariable String playbackId) {
        String userNickname = (String) sessionAttributes.get("userNickname");
        Long memberId = (Long) sessionAttributes.get("memberId");
        if (userNickname == null) {
            log.error("세션에서 userNickname을 찾을 수 없습니다. roomId: {}", playbackId);
            return;
        }

        chattingService.saveAndSend(request.getMessage(), memberId, playbackId);

    }
}
