package stack.moaticket.system.oauth.component;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;
import stack.moaticket.domain.oauth_info.repository.OauthInfoRepository;
import stack.moaticket.system.jwt.JwtUtil;
import stack.moaticket.system.oauth.service.OauthService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class Oauth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //loadUser를 통해 받은 oauth2유저 객체 정보, sub는 구글회원 유니크 값
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String oauthId = oAuth2User.getAttribute("sub").toString();
        OauthInfo existOauthInfo = oauthService.findOauthInfo(oauthId);
        long memberId;
        String role;

        //findOauthInfo를 통해 찾은 정보가 없을경우 Member테이블에 저장하고 MemberId를 가진 oauthinfo 테이블에 저장
        if (existOauthInfo == null) {
            //Member entity
            Member member = Member.builder()
                    .nickname(oAuth2User.getAttribute("name"))
                    .state(MemberState.ACTIVE)
                    .isSeller(false)
                    .email(oAuth2User.getAttribute("email"))
                    .build();
            memberService.joinMember(member);

            OauthInfo oauthInfo = OauthInfo.builder()
                    .member(member)
                    .oauthId(oauthId)
                    .build();
            oauthService.joinOauthInfo(oauthInfo);

            memberId = member.getId();
        } else {
            //존재하는 oauthinfo가 있을시 state를 기준으로 막을거 생각(탈퇴, 차단등)
            Member loggedInMember = memberService.findById(existOauthInfo.getMember().getId());
            memberId = loggedInMember.getId();
        }

        //24시간동안 토큰 발행
        String token = jwtUtil.createJwt(memberId, null, 60*60*1000*24L);

        response.addCookie(createCookie("Authorization", token));
        //로그인 성공 후 리다이렉트 되는 페이지
        response.sendRedirect("/");


    }
    private Cookie createCookie(String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 24);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

}
