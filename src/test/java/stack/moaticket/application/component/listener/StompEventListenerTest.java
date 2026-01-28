package stack.moaticket.application.component.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import stack.moaticket.application.component.registry.StompRoomRegistry;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompEventListenerTest {


    @Mock
    StompRoomRegistry stompRoomRegistry;
    @InjectMocks
    StompEventListener stompEventListener;

    @DisplayName("세션 연결 종료 시 registry에서 세션이 제거되는지 확인")
    @Test
    void disconnectHandle_unregistersSession(){
        //given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId("sessionId");
        Message<byte[]> message = MessageBuilder
                .withPayload(new byte[0])
                .setHeaders(accessor)
                .build();

        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "sessionId", CloseStatus.NORMAL);

        //when
        stompEventListener.disconnectHandle(event);

        //then
        verify(stompRoomRegistry).unregisterBySession("sessionId");
    }


}