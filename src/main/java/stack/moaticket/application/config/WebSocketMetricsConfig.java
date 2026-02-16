package stack.moaticket.application.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;
import stack.moaticket.application.component.registry.StompRoomRegistry;

@Configuration
@RequiredArgsConstructor
public class WebSocketMetricsConfig {

    private final WebSocketMessageBrokerStats stats;
    private final StompRoomRegistry stompRoomRegistry;

    @Bean
    public MeterBinder webSocketMeterBinder() {
        return registry -> {
            Gauge.builder("spring.websocket.sessions.current", () -> {
                var s = stats.getWebSocketSessionStats();
                return (s != null) ? (double) s.getWebSocketSessions() : 0.0;
            }).register(registry);
            // 현재 연결된 총 세션 수
            Gauge.builder("spring.websocket.sessions.total", () -> {
                var s = stats.getWebSocketSessionStats();
                return (s != null) ? (double) s.getTotalSessions() : 0.0;
            }).register(registry);

            // STOMP 구독 세션 수
            Gauge.builder("spring.websocket.stomp.sessions.connected", () -> {
                var s = stats.getStompSubProtocolStats();
                return (s != null) ? (double) s.getTotalConnected() : 0.0;
            }).register(registry);

            Gauge.builder("spring.websocket.stomp.sessions.connect", () -> {
                var s = stats.getStompSubProtocolStats();
                return (s != null) ? (double) s.getTotalConnect() : 0.0;
            }).register(registry);

            Gauge.builder("spring.websocket.stomp.sessions.disconnect", () -> {
                var s = stats.getStompSubProtocolStats();
                return (s != null) ? (double) s.getTotalDisconnect() : 0.0;
            }).register(registry);

            Gauge.builder("chat.stompRoomRegistry.session_room_map.size",
                            stompRoomRegistry::sessionRoomMapSize)
                    .description("sessionId -> roomId map size")
                    .register(registry);

            Gauge.builder("chat.stompRoomRegistry.session_member_map.size",
                            stompRoomRegistry::sessionMemberMapSize)
                    .description("sessionId -> memberId map size")
                    .register(registry);

            Gauge.builder("chat.stompRoomRegistry.ws_session_map.size",
                            stompRoomRegistry::wsSessionMapSize)
                    .description("websocket session map size")
                    .register(registry);
        };
    }
}