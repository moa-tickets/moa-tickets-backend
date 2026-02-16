package stack.moaticket.application.component.interceptor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WebSocketMessageMetricsInterceptor implements ChannelInterceptor {
    private final MeterRegistry meterRegistry;
    private static final String START_TIME_HEADER = "startTime";
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getCommand() != null) {
            // 메트릭 이름: websocket_messages_count
            // 태그를 달아두면 '발신/수신'을 구분해서 볼 수 있습니다.
            meterRegistry.counter("websocket.messages.count",
                    "command", accessor.getCommand().name()).increment();
            if (accessor.isMutable()) {
                accessor.setHeader(START_TIME_HEADER, System.currentTimeMillis());
            }
        }
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getCommand() != null) {
            Long startTime = (Long) accessor.getHeader(START_TIME_HEADER);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;

                // Micrometer Timer를 사용하여 지표 기록
                Timer.builder("websocket.message.duration")
                        .tag("command", accessor.getCommand().name())
                        .description("WebSocket 메시지 처리 지연 시간")
                        .register(meterRegistry)
                        .record(Duration.ofMillis(duration));
            }
        }
    }
}
