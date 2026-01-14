package stack.moaticket.application.component.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import stack.moaticket.application.component.registry.StompRoomRegistry;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler  implements ChannelInterceptor {

    private final StompRoomRegistry registry;
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);



        if(StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("connect 요청시 토큰 유효성 검증");
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            String jwt = (String) sessionAttributes.get("jwt");
            if (jwt != null) {
                // 토큰 검증 로직 수행  TODO
                log.info("쿠키에서 가져온 JWT: " + jwt);
            } else {
                throw new MoaException(MoaExceptionType.UNAUTHORIZED);
            }
        }

        if (accessor.getSessionId() != null && accessor.getCommand() != null) {
            registry.touch(accessor.getSessionId());
        }

        return message;
    }
}
