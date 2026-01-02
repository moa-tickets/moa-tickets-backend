package stack.moaticket.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;
import stack.moaticket.domain.member.type.MemberState;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberRepositoryQueryDsl memberRepositoryQueryDsl;

    public Member joinMember(String memberName, String memberEmail){
        Member member = Member.builder()
                .nickname(memberName)
                .state(MemberState.ACTIVE)
                .isSeller(false)
                .email(memberEmail)
                .build();
        return memberRepository.save(member);
    }

    public Member findById(long memberId){
        return memberRepositoryQueryDsl.findById(memberId);
    }
}
