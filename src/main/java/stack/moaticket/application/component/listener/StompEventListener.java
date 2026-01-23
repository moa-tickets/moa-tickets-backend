package stack.moaticket.application.component.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import stack.moaticket.application.component.registry.StompRoomRegistry;

import java.util.Map;


//스프링과 stomp는 기본적으로 세션관리를 자동으로 처리
//연결/해제 이벤트를 기록, 연결된 세션 수를 실시간으로 확인할 목적으로 이벤트 리스너 생서 >> 로그, 디버깅 목적
@Component
@Slf4j
@RequiredArgsConstructor
public class StompEventListener {

    private final StompRoomRegistry registry;


    @EventListener
    public void disconnectHandle(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> attrs = accessor.getSessionAttributes();
        String roomId = (String) attrs.get("roomId");
        registry.unregisterBySession(accessor.getSessionId());

        log.info("roomId : " + roomId);
        log.info("disconnect session Id : " + accessor.getSessionId());
        log.info("total sessions : " + registry.roomSize(roomId));
    }

}
