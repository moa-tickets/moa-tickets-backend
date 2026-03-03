package stack.moaticket.domain.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.member.entity.Member;

import java.util.Optional;

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

    public Member findByEmail(String email) {
        return jpaQueryFactory.selectFrom(member)
                .where(member.email.eq(email))
                .fetchOne();
    }

    public Member findByIdForUpdate(Long memberId) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(member.id.eq(memberId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();
    }

}
