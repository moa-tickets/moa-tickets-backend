package stack.moaticket.application.component.interceptor;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import stack.moaticket.system.jwt.JwtUtil;

import java.util.Map;


@Component
@Slf4j
@RequiredArgsConstructor
public class StompHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;


    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("Authorization".equals(cookie.getName())) {
                        try {
                            String token = cookie.getValue();
                            Long memberId = jwtUtil.getSubject(token);
                            attributes.put("X-Member-Id", memberId);
                            return true;
                        } catch (io.jsonwebtoken.JwtException e) {
                            log.error("웹소켓 인증 실패 : " + e.getMessage());
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}