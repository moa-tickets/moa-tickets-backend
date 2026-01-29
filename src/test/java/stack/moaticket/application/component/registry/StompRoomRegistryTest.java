package stack.moaticket.application.component.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import stack.moaticket.system.exception.MoaException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompRoomRegistryTest {


    @InjectMocks
    StompRoomRegistry stompRoomRegistry;

    @DisplayName("처음 채팅 접속")
    @Test
    void firstJoinChatroom() {
        // when
        String oldSession = stompRoomRegistry.register(1L, "sessionId", "roomId");
        // then
        assertThat(oldSession).isNull();
        assertThat(stompRoomRegistry.roomSize("roomId")).isEqualTo(1);
        assertThat(stompRoomRegistry.sessionMemberMapSize()).isEqualTo(1);
        assertThat(stompRoomRegistry.sessionMemberMapSize()).isEqualTo(1);
    }

    @DisplayName("같은 아이디로 같은 방 참여시 기존 세션 제거")
    @Test
    void joinSameChatroom() {
        // given
        stompRoomRegistry.register(1L, "sessionId1", "roomId");
        // when
        String oldSession = stompRoomRegistry.register(1L, "sessionId2", "roomId");
        // then
        assertThat(stompRoomRegistry.roomSize("roomId")).isEqualTo(1);
        assertThat(stompRoomRegistry.sessionRoomMapSize()).isEqualTo(1);
        assertThat(stompRoomRegistry.sessionRoomMapSize()).isEqualTo(1);
        assertThat(oldSession).isEqualTo("sessionId1");
    }

    @DisplayName("두명이 같은방 입장")
    @Test
    void joinSameChatroomTwoMember() {
        //given when
        stompRoomRegistry.register(1L, "sessionId1", "roomId");
        stompRoomRegistry.register(2L, "sessionId2", "roomId");
        // then
        assertThat(stompRoomRegistry.roomSize("roomId")).isEqualTo(2);
        assertThat(stompRoomRegistry.sessionRoomMapSize()).isEqualTo(2);
        assertThat(stompRoomRegistry.sessionMemberMapSize()).isEqualTo(2);
    }

    @DisplayName("두명이 입장 후 한명이 중복 입장")
    @Test
    void joinSameChatroomTwoMemberAndOneDuplicate() {
        // given
        stompRoomRegistry.register(1L, "sessionId1", "roomId");
        stompRoomRegistry.register(2L, "sessionId2", "roomId");
        // when
        stompRoomRegistry.register(2L, "sessionId3", "roomId");
        // then
        assertThat(stompRoomRegistry.roomSize("roomId")).isEqualTo(2);
        assertThat(stompRoomRegistry.sessionRoomMapSize()).isEqualTo(2);
        assertThat(stompRoomRegistry.sessionMemberMapSize()).isEqualTo(2);
    }


    @DisplayName("세션 등록해제")
    @Test
    void unregisterBySession() {
        // given
        stompRoomRegistry.register(1L, "sessionId1", "roomId");
        // when
        stompRoomRegistry.unregisterBySession("sessionId1");
        // then
        assertThat(stompRoomRegistry.roomSize("roomId")).isEqualTo(0);
        assertThat(stompRoomRegistry.sessionRoomMapSize()).isEqualTo(0);
        assertThat(stompRoomRegistry.sessionMemberMapSize()).isEqualTo(0);
    }

    @DisplayName("웹소켓 세션 등록")
    @Test
    void registerWsSessionTest() {
        //given
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("sessionId");
        //when
        stompRoomRegistry.registerWsSession(ws);

        assertThat(stompRoomRegistry.wsSessionMapSize()).isEqualTo(1);
    }

    @DisplayName("웹소켓 세션 종료")
    @Test
    void closeWsSessionTest() throws IOException {
        // given
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("sessionId");
        when(ws.isOpen()).thenReturn(true);
        stompRoomRegistry.registerWsSession(ws);
        // when
        stompRoomRegistry.closeSession("sessionId", CloseStatus.POLICY_VIOLATION);
        // then
        verify(ws, times(1)).close(CloseStatus.POLICY_VIOLATION);
    }

    @DisplayName("웹소켓 세션 해제")
    @Test
    void unregisterWsSessionTest() {
        // given
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("sessionId");
        stompRoomRegistry.registerWsSession(ws);
        // when
        stompRoomRegistry.unregisterWsSession("sessionId");
        // then
        assertThat(stompRoomRegistry.wsSessionMapSize()).isEqualTo(0);
    }


    @DisplayName("touch 작동 테스트")
    @Test
    void touchTest() {
        // when
        stompRoomRegistry.register(1L, "sessionId", "room1");
        // then
        assertThatCode(() -> stompRoomRegistry.touch("sessionId"))
                .doesNotThrowAnyException();
    }


    @DisplayName("예외처리 시 세션 닫힘")
    @Test
    void closeSession_whenWsCloseThrowsException_rethrowsAsMoaException() throws Exception {
        // given
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("sessionId");
        when(ws.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("close fail"))
                .when(ws).close(any());
        stompRoomRegistry.registerWsSession(ws);
        // when then
        assertThatCode(() ->
                stompRoomRegistry.closeSession("sessionId", CloseStatus.POLICY_VIOLATION)
        ).isInstanceOf(MoaException.class)
                .hasMessage("서버 내부 오류");
    }

    @DisplayName("touch 아이디 null일 시 예외처리")
    @Test
    void touchIdIsNull() {
        // then
        assertThatThrownBy(() -> stompRoomRegistry.touch("sessionId2"))
                .isInstanceOf(MoaException.class)
                .hasMessage("요청한 리소스를 찾을 수 없습니다.");
    }


}