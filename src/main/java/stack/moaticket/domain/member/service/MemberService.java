package stack.moaticket.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

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
    public Member getByIdOrThrow(long memberId) {
        Member member = memberRepositoryQueryDsl.findById(memberId);
        if (member == null) {
            throw new MoaException(MoaExceptionType.MEMBER_NOT_FOUND);
        }
        return member;
    }

    public Member convertToSeller(long memberId){
        Member member = memberRepositoryQueryDsl.findById(memberId);
        member.promoteToSeller();
        return memberRepository.save(member);
    }

    public Member convertToBuyer(long memberId){
        Member member = memberRepositoryQueryDsl.findById(memberId);
        member.demoteFromSeller();
        return memberRepository.save(member);
    }
}
