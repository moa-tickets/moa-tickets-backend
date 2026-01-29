package stack.moaticket.application.component.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.CloseStatus;
import stack.moaticket.application.component.registry.StompRoomRegistry;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompHandlerTest {

    @Mock
    MemberService memberService;
    @Mock
    StompRoomRegistry registry;
    @InjectMocks
    StompHandler stompHandler;

    private Message<byte[]> buildMessage(
            StompCommand command,
            String sessionId,
            Map<String, Object> sessionAttr,
            Map<String, String> nativeHeaders
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);

        if (sessionAttr != null) {
            accessor.setSessionAttributes(sessionAttr);
        }

        if (nativeHeaders != null) {
            nativeHeaders.forEach(accessor::setNativeHeader);
        }

        MessageHeaders headers = accessor.getMessageHeaders();
        return MessageBuilder.createMessage(new byte[0], headers);
    }

    @Test
    @DisplayName("accessor 또는 command가 null이면 message를 그대로 반환")
    void preSend_returnsOriginalMessage_whenAccessorOrCommandNull() {
        // given
        Message<byte[]> msg = MessageBuilder.withPayload(new byte[0]).build();
        // when
        Message<?> result = stompHandler.preSend(msg, null);
        // then
        assertSame(msg, result);
    }

    @Test
    @DisplayName("CONNECT: 세션/헤더 검증 후 nickname/memberId/roomId 세션에 세팅 + X-Member-Id 제거")
    void preSend_connect_success_setsSessionAttributes() {
        // given
        Map<String, Object> session = new HashMap<>();
        session.put("X-Member-Id", 10L);
        Map<String, String> nativeHeaders = new HashMap<>();
        nativeHeaders.put("roomId", "roomId");
        Member member = mock(Member.class);
        when(memberService.findById(10L)).thenReturn(member);
        when(member.getNickname()).thenReturn("nick");
        Message<byte[]> msg = buildMessage(StompCommand.CONNECT, "sessionId", session, nativeHeaders);
        // when
        stompHandler.preSend(msg, null);
        // then
        assertNull(session.get("X-Member-Id"), "보안상 X-Member-Id는 제거되어야 함");
        assertEquals("nick", session.get("userNickname"));
        assertEquals(10L, session.get("memberId"));
        assertEquals("roomId", session.get("roomId"));
        verify(memberService, times(1)).findById(10L);
    }

    @Test
    @DisplayName("CONNECT: memberId 또는 roomId가 없으면 VALIDATION_FAILED")
    void preSend_connect_validationFailed_whenMissingMemberIdOrRoomId() {
        // given
        Map<String, Object> session1 = new HashMap<>();
        Map<String, String> nativeHeaders1 = new HashMap<>();
        nativeHeaders1.put("roomId", "roomId");
        Message<byte[]> msg1 = buildMessage(StompCommand.CONNECT, "sessionId", session1, nativeHeaders1);
        // when
        MoaException ex1 = assertThrows(MoaException.class, () -> stompHandler.preSend(msg1, null));
        // then
        assertEquals(MoaExceptionType.VALIDATION_FAILED, ex1.getType());
        assertEquals("요청 값이 올바르지 않습니다.", ex1.getMessage());

        // roomId 없음
        Map<String, Object> session2 = new HashMap<>();
        session2.put("X-Member-Id", 10L);
        Message<byte[]> msg2 = buildMessage(StompCommand.CONNECT, "sessionId", session2, new HashMap<>());
        // when
        MoaException ex2 = assertThrows(MoaException.class, () -> stompHandler.preSend(msg2, null));
        // then
        assertEquals(MoaExceptionType.VALIDATION_FAILED, ex2.getType());
        assertEquals("요청 값이 올바르지 않습니다.", ex2.getMessage());
    }

    @Test
    @DisplayName("CONNECT: memberService에서 null 반환이면 MEMBER_NOT_FOUND")
    void preSend_connect_memberNotFound_whenMemberNull() {
        // given
        Map<String, Object> session = new HashMap<>();
        session.put("X-Member-Id", 10L);
        Map<String, String> nativeHeaders = new HashMap<>();
        nativeHeaders.put("roomId", "roomId");
        when(memberService.findById(10L)).thenReturn(null);
        Message<byte[]> msg = buildMessage(StompCommand.CONNECT, "sessionId", session, nativeHeaders);
        // when
        MoaException ex = assertThrows(MoaException.class, () -> stompHandler.preSend(msg, null));
        // then
        assertEquals(MoaExceptionType.MEMBER_NOT_FOUND, ex.getType());
        assertEquals("올바른 사용자를 찾을 수 없습니다", ex.getMessage());
    }

    @Test
    @DisplayName("SUBSCRIBE: registry.register 결과가 다른 oldSessionId면 closeSession 호출 + touch 호출")
    void preSend_subscribe_duplicateSession_closesOldSession_andTouches() {
        // given
        Map<String, Object> session = new HashMap<>();
        session.put("roomId", "roomId");
        session.put("memberId", 10L);
        when(registry.register(10L, "newSessionId", "roomId")).thenReturn("oldSessionId");
        Message<byte[]> msg = buildMessage(StompCommand.SUBSCRIBE, "newSessionId", session, null);
        // when
        stompHandler.preSend(msg, null);
        // then
        verify(registry, times(1)).register(10L, "newSessionId", "roomId");
        verify(registry, times(1)).closeSession("oldSessionId", CloseStatus.POLICY_VIOLATION);
        verify(registry, times(1)).touch("newSessionId");
    }

    @Test
    @DisplayName("SUBSCRIBE: oldSessionId가 null이면 closeSession은 호출하지 않음 + touch 호출")
    void preSend_subscribe_noDuplicate_onlyTouches() {
        // given
        Map<String, Object> session = new HashMap<>();
        session.put("roomId", "roomId");
        session.put("memberId", 10L);
        when(registry.register(10L, "newSessionId", "roomId")).thenReturn(null);
        Message<byte[]> msg = buildMessage(StompCommand.SUBSCRIBE, "newSessionId", session, null);
        // when
        stompHandler.preSend(msg, null);
        // then
        verify(registry, times(1)).register(10L, "newSessionId", "roomId");
        verify(registry, never()).closeSession(anyString(), any());
        verify(registry, times(1)).touch("newSessionId");
    }

    @Test
    @DisplayName("SUBSCRIBE: memberId/roomId 없으면 VALIDATION_FAILED")
    void preSend_subscribe_validationFailed_whenMissingAttrs() {
        // given
        Map<String, Object> session = new HashMap<>();
        session.put("roomId", "roomId"); // memberId 없음
        Message<byte[]> msg = buildMessage(StompCommand.SUBSCRIBE, "newSessionId", session, null);
        // when
        MoaException ex = assertThrows(MoaException.class, () -> stompHandler.preSend(msg, null));
        // then
        assertEquals(MoaExceptionType.VALIDATION_FAILED, ex.getType());
    }



    @Test
    @DisplayName("SEND: sendTime이 세션에 저장됨")
    void preSend_send_setsSendTime() {
        // given
        Map<String, Object> session = new HashMap<>();
        Message<byte[]> msg = buildMessage(StompCommand.SEND, "s3", session, null);
        // when
        stompHandler.preSend(msg, null);
        // then
        assertTrue(session.containsKey("sendTime"));
        assertNotNull(session.get("sendTime"));
        assertTrue(session.get("sendTime") instanceof LocalDateTime);
    }

    @Test
    @DisplayName("DISCONNECT: 예외 없이 통과")
    void preSend_disconnect_noop() {
        // given
        Map<String, Object> session = new HashMap<>();
        Message<byte[]> msg = buildMessage(StompCommand.DISCONNECT, "s4", session, null);
        // when then
        assertDoesNotThrow(() -> stompHandler.preSend(msg, null));
    }
}
