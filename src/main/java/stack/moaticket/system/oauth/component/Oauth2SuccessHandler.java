package stack.moaticket.system.oauth.component;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;
import stack.moaticket.system.jwt.JwtUtil;
import stack.moaticket.domain.oauth_info.service.OauthInfoService;
import stack.moaticket.system.oauth.facade.OauthFacade;

import java.io.IOException;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class Oauth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OauthInfoService oauthService;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final OauthFacade oauthFacade;

    @Value("${spring.profiles.active}")
    private String profile;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.cookie.domain}")
    private String cookieDomain;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //loadUser를 통해 받은 oauth2유저 객체 정보, sub는 구글회원 유니크 값
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String oauthId = oAuth2User.getAttribute("sub").toString();
        OauthInfo existOauthInfo = oauthService.findOauthInfo(oauthId);
        long memberId;

        //findOauthInfo를 통해 찾은 정보가 없을경우 name과 email을 이용해 member, oauthinfo insert 진행
        if (existOauthInfo == null) {
            //Member entity
            String name = oAuth2User.getAttribute("name");
            String email = oAuth2User.getAttribute("email");
            memberId = oauthFacade.signUp(name, oauthId, email);

        } else {
            //존재하는 oauthinfo가 있을시 state를 기준으로 막을거 생각(탈퇴, 차단등)
            Member loggedInMember = memberService.findById(existOauthInfo.getMember().getId());
            if(loggedInMember == null) throw new RuntimeException(); // TODO
            memberId = loggedInMember.getId();
        }

        //24시간동안 토큰 발행
        String token = jwtUtil.createJwt(memberId,60*60*1000*24L);

        response.addHeader("Set-Cookie", createCookie(token).toString());

        //로그인 성공 후 리다이렉트 되는 페이지
        response.sendRedirect(frontendUrl + "/login-callback");



    }
    private ResponseCookie createCookie(String token){
        if(profile.equals("dev")) {
            return ResponseCookie.from("Authorization", token)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(60 * 60 * 24)
                    .build();
        }
        else {
            return ResponseCookie.from("Authorization", token)
                    .secure(true)
                    .sameSite("Lax")
                    .path("/")
                    .domain(cookieDomain)
                    .maxAge(60 * 60 * 24)
                    .build();
        }
    }

}
