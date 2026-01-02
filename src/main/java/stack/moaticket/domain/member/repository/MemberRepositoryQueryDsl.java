package stack.moaticket.domain.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.member.entity.Member;

import static stack.moaticket.domain.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    public Member findById(long id){
        return jpaQueryFactory.selectFrom(member)
                .where(member.id.eq(id))
                .fetchOne();
    }
}
