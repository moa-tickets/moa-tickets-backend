package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import settings.support.util.TestUtil;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.member.type.MemberState;

public class MemberFixture extends BaseFixture<Member, Long> {
    private final MemberRepository memberRepository;

    public MemberFixture(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    protected JpaRepository<Member, Long> repo() {
        return memberRepository;
    }

    @Transactional
    public Member create() {
        return save(Member.builder()
                .nickname(TestUtil.uniqueString("nickname"))
                .email(TestUtil.uniqueEmail())
                .isSeller(true)
                .state(MemberState.ACTIVE)
                .build());
    }
}
