package stack.moaticket.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.system.exception.MoaException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberInfoServiceTest {

    @Mock
    MemberService memberService;
    @InjectMocks MemberInfoService memberInfoService;

    @DisplayName("getMember 회원 가져오기 실패 테스트")
    @Test
    void getMemberFailMemberNotFound(){
        //given
        Long memberId = 1L;
        when(memberService.findById(memberId)).thenReturn(null);
        //when
        assertThatThrownBy(() ->
                memberInfoService.getMember(memberId)
        ).isInstanceOf(MoaException.class)
                .hasMessage("올바른 사용자를 찾을 수 없습니다");
        //then

    }


}