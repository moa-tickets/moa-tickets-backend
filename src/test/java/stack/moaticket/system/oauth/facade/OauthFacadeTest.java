package stack.moaticket.system.oauth.facade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.oauth_info.service.OauthInfoService;
import stack.moaticket.domain.member.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OauthFacadeTest {
    @Mock
    MemberService memberService;
    @Mock
    OauthInfoService oauthInfoService;

    @InjectMocks
    OauthFacade oauthFacade;

    @DisplayName("signUp진행시 memberId 리턴")
    @Test
    void signUpReturnMemberId(){
        // given
        String name = "name";
        String oauthId = "oauthId";
        String email = "email";
        Member member = mock(Member.class);
        when(memberService.joinMember(name, email)).thenReturn(member);
        when(member.getId()).thenReturn(1L);
        // when
        Long getMemberId = oauthFacade.signUp(name, oauthId, email);
        // then
        assertThat(getMemberId).isEqualTo(1L);
    }


}