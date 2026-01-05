package stack.moaticket.domain.ticket_hold.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.ticket_hold.entity.TicketHold;

import java.time.LocalDateTime;
import java.util.List;

import static stack.moaticket.domain.ticket_hold.entity.QTicketHold.ticketHold;

@Repository
@RequiredArgsConstructor
public class TicketHoldRepositoryQueryDsl {

    private final JPAQueryFactory queryFactory;

    public long deleteExpired(LocalDateTime now) {
        return queryFactory.delete(ticketHold)
                .where(ticketHold.expiresAt.loe(now))
                .execute();
    }

    public long deleteByMemberAndSession(Long memberId, Long sessionId) {
        return queryFactory.delete(ticketHold)
                .where(ticketHold.memberId.eq(memberId)
                        .and(ticketHold.sessionId.eq(sessionId)))
                .execute();
    }

    public long deleteByHoldToken(String holdToken) {
        return queryFactory.delete(ticketHold)
                .where(ticketHold.holdToken.eq(holdToken))
                .execute();
    }

    public List<TicketHold> findByHoldToken(String holdToken) {
        return queryFactory.selectFrom(ticketHold)
                .where(ticketHold.holdToken.eq(holdToken))
                .fetch();
    }

    public List<Long> findHeldTicketIdsBySession(Long sessionId, LocalDateTime now) {
        return queryFactory.select(ticketHold.ticketId)
                .from(ticketHold)
                .where(ticketHold.sessionId.eq(sessionId)
                        .and(ticketHold.expiresAt.gt(now)))
                .fetch();
    }
}
