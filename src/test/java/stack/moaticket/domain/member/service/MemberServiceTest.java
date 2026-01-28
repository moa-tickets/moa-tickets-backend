package stack.moaticket.domain.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    MemberRepositoryQueryDsl memberRepositoryQueryDsl;
    @InjectMocks
    MemberService memberService;

    @DisplayName("joinMember 정상 작동")
    @Test
    void joinMember(){
        //given
        Member member = mock(Member.class);
        String name = "name";
        String email = "email";
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        //when
        Member saved = memberService.joinMember(name, email);
        //then
        assertThat(saved.getNickname()).isEqualTo(name);
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getState()).isEqualTo(MemberState.ACTIVE);
        assertThat(saved.isSeller()).isFalse();

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository, times(1)).save(captor.capture());
        Member toSave = captor.getValue();

        assertThat(toSave.getNickname()).isEqualTo(name);
        assertThat(toSave.getEmail()).isEqualTo(email);
        assertThat(toSave.getState()).isEqualTo(MemberState.ACTIVE);
        assertThat(toSave.isSeller()).isFalse();
    }

    @DisplayName("findById 실행시 결과값 반환")
    @Test
    void findById_getQueryResult() {
        // given
        Member member = mock(Member.class);
        when(memberRepositoryQueryDsl.findById(1L)).thenReturn(member);

        // when
        Member found = memberService.findById(1L);

        // then
        assertThat(found).isSameAs(member);
        verify(memberRepositoryQueryDsl).findById(1L);
        verifyNoInteractions(memberRepository);
    }

    @DisplayName("getByIdOrThrow: 존재하면 Member 반환")
    @Test
    void getByIdOrThrow_returnsMember_whenExists() {
        // given
        Member member = mock(Member.class);
        when(memberRepositoryQueryDsl.findById(1L)).thenReturn(member);

        // when
        Member found = memberService.getByIdOrThrow(1L);

        // then
        assertThat(found).isSameAs(member);
        verify(memberRepositoryQueryDsl).findById(1L);
        verifyNoInteractions(memberRepository);
    }

    @DisplayName("getByIdOrThrow: 없으면 MEMBER_NOT_FOUND 예외")
    @Test
    void getByIdOrThrow_throws_whenNull() {
        // given
        when(memberRepositoryQueryDsl.findById(99L)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> memberService.getByIdOrThrow(99L))
                .isInstanceOf(MoaException.class)
                .hasMessage("올바른 사용자를 찾을 수 없습니다");

        verify(memberRepositoryQueryDsl).findById(99L);
        verifyNoInteractions(memberRepository);
    }

}