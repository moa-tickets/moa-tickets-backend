package stack.moaticket.system.oauth.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.oauth_info.service.OauthInfoService;
import stack.moaticket.domain.member.entity.Member;

@Component
@RequiredArgsConstructor
public class OauthFacade {
    private final MemberService memberService;
    private final OauthInfoService oauthInfoService;

    @Transactional
    public Long signUp(String name, String oauthId, String email) {
        Member member = memberService.joinMember(name, email);
        oauthInfoService.joinOauthInfo(member, oauthId);
        return member.getId();
    }
}
