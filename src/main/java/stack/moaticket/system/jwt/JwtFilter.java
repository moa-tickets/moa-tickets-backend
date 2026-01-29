package stack.moaticket.system.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import stack.moaticket.application.util.FilterUtil;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.io.IOException;
import java.util.Arrays;
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestUri = request.getRequestURI();
            boolean isPass = FilterUtil.checkFilter(requestUri);

            if(isPass) {
                filterChain.doFilter(request, response);
                return;
            }

            String authorization = null;
            Cookie[] cookies = request.getCookies();

            if(cookies != null) {
                authorization = Arrays.stream(cookies)
                        .filter(cookie -> cookie.getName().equals("Authorization"))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }

            if(authorization == null){
                System.out.println("token null");
                throw new RuntimeException(); // TODO
            }

            String token = authorization;

            long memberId = jwtUtil.getSubject(token);

            Authentication authToken = new UsernamePasswordAuthenticationToken(memberId, null, null);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        } catch (MalformedJwtException e) {
            log.warn(e.getMessage());
            throw new MoaException(MoaExceptionType.MISMATCH_TOKEN);
        } catch (ExpiredJwtException e) {
            log.info(e.getMessage());
            throw new MoaException(MoaExceptionType.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException | SignatureException e) {
            log.warn(e.getMessage());
            throw new MoaException(MoaExceptionType.INVALID_TOKEN);
        } catch (RuntimeException e) {
            log.warn(e.getMessage());
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
    }


}
