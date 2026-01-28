package stack.moaticket.system.oauth.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;
import stack.moaticket.domain.oauth_info.service.OauthInfoService;
import stack.moaticket.system.jwt.JwtUtil;
import stack.moaticket.system.oauth.facade.OauthFacade;
import stack.moaticket.domain.member.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Oauth2SuccessHandlerTest {
    @Mock
    OauthInfoService oauthInfoService;
    @Mock
    Member member;
    @Mock
    OauthInfo oauthInfo;
    @Mock
    MemberService memberService;
    @Mock
    JwtUtil jwtUtil;
    @Mock
    OauthFacade oauthFacade;
    @Mock
    OAuth2User oauth2User;
    @Mock
    Authentication authentication;
    @InjectMocks
    Oauth2SuccessHandler handler;

    @DisplayName("신규 회원 로그인시 회원가입 처리")
    @Test
    void newMemberLogin() throws Exception {
        //given
        ReflectionTestUtils.setField(handler, "profile", "dev");
        ReflectionTestUtils.setField(handler, "frontendUrl", "https://moatickets.dev");
        when(oauth2User.getAttribute("sub")).thenReturn("oauthId");
        when(oauth2User.getAttribute("name")).thenReturn("name");
        when(oauth2User.getAttribute("email")).thenReturn("email");

        when(authentication.getPrincipal()).thenReturn(oauth2User);

        when(oauthInfoService.findOauthInfo("oauthId")).thenReturn(null);
        when(oauthFacade.signUp("name", "oauthId", "email")).thenReturn(1L);
        when(jwtUtil.createJwt(1L, 60 * 60 * 1000 * 24L)).thenReturn("token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        //when
        handler.onAuthenticationSuccess(request, response, authentication);
        //then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotBlank();
        assertThat(setCookie).contains("Authorization=token");

        assertThat(response.getRedirectedUrl()).isEqualTo("https://moatickets.dev/login-callback");

        verify(oauthFacade).signUp("name", "oauthId", "email");
        verify(jwtUtil).createJwt(1L, 60 * 60 * 1000 * 24L);
    }

    @DisplayName("기존 회원 로그인시 회원가입 안함.")
    @Test
    void existMemberLogin() throws Exception {
        //given
        ReflectionTestUtils.setField(handler, "profile", "dev");
        ReflectionTestUtils.setField(handler, "frontendUrl", "https://moatickets.dev");
        when(oauth2User.getAttribute("sub")).thenReturn("oauthId");

        when(authentication.getPrincipal()).thenReturn(oauth2User);

        when(oauthInfo.getMember()).thenReturn(member);
        when(member.getId()).thenReturn(1L);
        when(oauthInfoService.findOauthInfo("oauthId")).thenReturn(oauthInfo);
        when(memberService.findById(1L)).thenReturn(member);
        when(jwtUtil.createJwt(1L, 60 * 60 * 1000 * 24L)).thenReturn("token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        //when
        handler.onAuthenticationSuccess(request, response, authentication);
        //then
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotBlank();
        assertThat(setCookie).contains("Authorization=token");

        assertThat(response.getRedirectedUrl()).isEqualTo("https://moatickets.dev/login-callback");
        verify(oauthFacade, never()).signUp("name", "oauthId", "email");
        verify(jwtUtil).createJwt(1L, 60 * 60 * 1000 * 24L);
    }
}