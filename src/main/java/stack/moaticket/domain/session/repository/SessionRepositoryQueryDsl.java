package stack.moaticket.domain.session.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import static stack.moaticket.domain.session.entity.QSession.session;

import stack.moaticket.domain.session.entity.Session;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryQueryDsl {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Session> getSessions(long concertId){
        return jpaQueryFactory
                .selectFrom(session)
                .where(session.concert.id.eq(concertId))
                .fetch();
    }

    public Session getSessionById(long sessionId){
        return jpaQueryFactory
                .selectFrom(session)
                .where(session.id.eq(sessionId))
                .fetchOne();
    }

}
