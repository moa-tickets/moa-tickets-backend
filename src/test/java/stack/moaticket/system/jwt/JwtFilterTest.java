package stack.moaticket.system.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import stack.moaticket.system.exception.MoaException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    JwtUtil jwtUtil;
    @InjectMocks
    JwtFilter filter;

    @DisplayName("doFilter 작동 여부")
    @Test
    void when_passUri_then_skipAuthAndDoFilter() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        // when
        filter.doFilter(request, response, chain);
        // then
        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @DisplayName("토큰값이 없을때 UNAUTHORIZED에러 발생")
    @Test
    void tokenNull() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain .class);
        // when then
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(MoaException.class)
                .hasMessage("인증되지 않은 사용자입니다.");
    }

    @DisplayName("토큰값이 잘못된 토큰값일때 에러 발생 테스트")
    @Test
    void malformedToken() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("Authorization", "token"));
        FilterChain chain = mock(FilterChain .class);
        when(jwtUtil.getSubject("token")).thenThrow(new MalformedJwtException("malformed"));
        // when then
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(MoaException.class)
                .hasMessage("토큰 형식이 잘못되었습니다.");
    }

    @DisplayName("토큰값의 유효기간이 종료되었을때 에러발생 테스트")
    @Test
    void expiredToken() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("Authorization", "token"));
        FilterChain chain = mock(FilterChain .class);
        when(jwtUtil.getSubject("token")).thenThrow(mock(ExpiredJwtException.class));
        // when then
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(MoaException.class)
                .hasMessage("만료된 토큰입니다.");
    }

    @DisplayName("지원하지않은 토큰을 보냈을때 에러발생 테스트")
    @Test
    void unsupportedToken() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("Authorization", "token"));
        FilterChain chain = mock(FilterChain .class);
        when(jwtUtil.getSubject("token")).thenThrow(mock(UnsupportedJwtException.class));
        // when then
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(MoaException.class)
                .hasMessage("잘못된 토큰입니다.");
    }

    @DisplayName("잘못된 서명 토큰 에러 테스트")
    @Test
    void invalidSignatureToken() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("Authorization", "token"));
        FilterChain chain = mock(FilterChain .class);
        when(jwtUtil.getSubject("token")).thenThrow(mock(SignatureException.class));
        // when then
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(MoaException.class)
                .hasMessage("잘못된 토큰입니다.");
    }

    @DisplayName("쿠키값을 통해 authorization설정")
    @Test
    void getCookieAndAuthorization() throws Exception {
        // given
        when(jwtUtil.getSubject("token")).thenReturn(1L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/test");
        request.setCookies(new Cookie("Authorization", "token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        // when
        filter.doFilter(request, response, chain);
        // then
        var oauth = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(oauth);
        assertEquals(1L, oauth.getPrincipal());
        verify(chain, times(1)).doFilter(request, response);
        verify(jwtUtil, times(1)).getSubject("token");
    }


}