package stack.moaticket.domain.member.service;

import org.springframework.stereotype.Service;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberRepositoryQueryDsl memberRepositoryQueryDsl;

    public MemberService(MemberRepository memberRepository, MemberRepositoryQueryDsl memberRepositoryQueryDsl){
        this.memberRepository = memberRepository;
        this.memberRepositoryQueryDsl = memberRepositoryQueryDsl;
    }

    public Member joinMember(Member member){
        return memberRepository.save(member);
    }

    public Member findById(long memberId){
        return memberRepositoryQueryDsl.findById(memberId);
    }
}
