package stack.moaticket.application.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import stack.moaticket.application.component.handler.StompHandler;
import stack.moaticket.application.component.interceptor.StompHandshakeInterceptor;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class StompWebsocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompHandler stompHandler;
    private final StompHandshakeInterceptor stompHandshakeInterceptor;
//    private final WebsocketHandshakeInterceptor websocketHandshakeInterceptor;
//    private final StompChannelInterceptor stompChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /publish로 시작하는 url 패턴으로 메세지가 발행되면 @Controller 객체의 @MessageMapping메서드로 라우팅
        registry.setApplicationDestinationPrefixes("/pub")
                .enableSimpleBroker("/sub", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(heartBeatScheduler());
        registry.setUserDestinationPrefix("/user");
    }

    @Bean
    public TaskScheduler heartBeatScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    //웹소켓 요청(connect, sub, disconnect)등의 요청시에는 http heade등 http메세지를 넣어올 수 있고 이를 interceptor를 통해 가로채 토큰등을 검증할 수 있음
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/connect")
                .addInterceptors(stompHandshakeInterceptor)
                .setAllowedOriginPatterns("*") // TODO
                .withSockJS(); //ws://가 아닌 http://엔드 포인트를 사용할 수 있게 해주는 sockJs를 통한 요청을 혀용하는 설정
    }
}