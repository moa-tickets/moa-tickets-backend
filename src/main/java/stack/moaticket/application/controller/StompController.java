package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.application.facade.ChattingFacade;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StompController {

    private final ChattingFacade chattingFacade;

    @MessageMapping("/send/{playbackId}")
    public void sendMessage(
            @Header("simpSessionAttributes") Map<String, Object> sessionAttributes,
            ChattingDto.Request request,
            @DestinationVariable String playbackId) {
        Long memberId = (Long) sessionAttributes.get("memberId");
        LocalDateTime sendTime = (LocalDateTime) sessionAttributes.get("sendTime");
        String subscribedRoom = (String) sessionAttributes.get("roomId");

        if (memberId == null) {
            log.error("세션에서 memberId를 찾을 수 없습니다. playbackId: {}", playbackId);
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        if (!subscribedRoom.equals(playbackId)) {
            log.error("구독하지 않은 방으로의 채팅 구독 : {}, 채팅 : {}, 사용자 : {}", subscribedRoom, playbackId, memberId);
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }


        chattingFacade.saveAndSend(request.getMessage(), memberId, playbackId, sendTime);

    }
}
