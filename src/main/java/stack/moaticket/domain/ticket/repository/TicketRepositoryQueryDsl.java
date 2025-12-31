package stack.moaticket.domain.ticket.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.ticket.entity.Ticket;

import java.util.List;

import static stack.moaticket.domain.ticket.entity.QTicket.ticket;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    // 여러 티켓 조회(비관적 락). 데드락 방지를 위해 id 기준 정렬
    public List<Ticket> getTicketWithLock(List<Long> ticketIds) {
        return jpaQueryFactory.selectFrom(ticket)
                .where(ticket.id.in(ticketIds))
                .orderBy(ticket.id.asc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    // 회차별 티켓 목록 조회(좌석 배치도용)
    public List<Ticket> getTicketsBySession(long sessionId) {
        return jpaQueryFactory.selectFrom(ticket)
                .where(ticket.session.id.eq(sessionId))
                .orderBy(ticket.num.asc())
                .fetch();
    }
}
