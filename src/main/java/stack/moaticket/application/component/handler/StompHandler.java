package stack.moaticket.application.component.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import stack.moaticket.application.component.registry.StompRoomRegistry;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler  implements ChannelInterceptor {
    private final MemberService memberService;
    private final StompRoomRegistry registry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        String newSessionId = accessor.getSessionId();



            // 2. 명령어별 로직 분기
        switch (accessor.getCommand()) {
            case CONNECT:
                log.info("[WS] CONNECT 요청 - 세션 및 토큰 검증 시작");

                if (sessionAttributes != null) {
                    String roomId = accessor.getFirstNativeHeader("roomId");
                    Long memberId = (Long) sessionAttributes.get("X-Member-Id");
                    if (memberId == null || roomId == null) {
                        throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
                    }

                    // 사용 후 보안 및 깔끔한 관리를 위해 제거
                    sessionAttributes.remove("X-Member-Id");

                    log.info("[WS] CONNECT 세부정보 -> sessionId: {}, roomId: {}, memberId: {}", newSessionId, roomId, memberId);

                    // 닉네임 조회 및 세션 저장
                    Member member = memberService.findById(memberId);
                    if (member == null) {
                        throw new MoaException(MoaExceptionType.MEMBER_NOT_FOUND);
                    }

                    String nickname = member.getNickname();
                    sessionAttributes.put("memberNickname", nickname);
                    sessionAttributes.put("memberId", memberId);
                    sessionAttributes.put("roomId", roomId);

                }
                break;

            case SUBSCRIBE:
                log.info("[WS] SUBSCRIBE 요청 - sessionId: {}", newSessionId);

                String roomId = (String) sessionAttributes.get("roomId");
                Long memberId = (Long) sessionAttributes.get("memberId");
                if (memberId == null || roomId == null) throw new MoaException(MoaExceptionType.VALIDATION_FAILED);


                String oldSessionId = registry.register(memberId, newSessionId, roomId);
//                if (oldSessionId != null && !oldSessionId.equals(newSessionId)) {
//                    registry.closeSession(oldSessionId, CloseStatus.POLICY_VIOLATION);
//                    log.info("같은 방 중복 접속 차단 memberId={}, roomId={}, oldSessionId={}", memberId, roomId, oldSessionId);
//                }
                registry.touch(newSessionId);
                // 필요 시 특정 방 구독 권한 체크 로직 추가 가능
                break;

            case SEND:
                log.info("[WS] SEND 메시지 전송 - sessionId: {}", newSessionId);
                LocalDateTime sendTime = LocalDateTime.now();
                sessionAttributes.put("sendTime", sendTime);
                // 도배 방지(Rate Limit) 로직 추가 가능
                break;

            case DISCONNECT:
                log.info("[WS] DISCONNECT 요청 - sessionId: {}", newSessionId);
                // 명시적 연결 종료 처리

                break;

            default:
                break;
        }


        return message;
    }
}
