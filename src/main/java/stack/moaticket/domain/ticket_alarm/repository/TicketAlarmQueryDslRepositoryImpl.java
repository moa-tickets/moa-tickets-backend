package stack.moaticket.domain.ticket_alarm.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket_alarm.dto.TicketAlarmDto;

import java.util.List;

import static stack.moaticket.domain.ticket_alarm.entity.QTicketAlarm.ticketAlarm;

@Repository
@RequiredArgsConstructor
public class TicketAlarmQueryDslRepositoryImpl implements TicketAlarmQueryDslRepository{
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public void delete(Member member, Ticket ticket, Long ticketAlarmId) {
        Long memberId = member.getId();
        Long ticketId = ticket.getId();

        BooleanExpression condition = ticketAlarm.subscriber.id.eq(memberId)
                .and(ticketAlarm.ticket.id.eq(ticketId));

        jpaQueryFactory.delete(ticketAlarm)
                .where(condition)
                .execute();
    }

    @Override
    public List<TicketAlarmDto> getReceiverDtoList(List<Long> ticketIdList, Long cursor, int limit) {
        BooleanExpression condition = ticketAlarm.ticket.id.in(ticketIdList);

        if(cursor != null) condition = condition.and(ticketAlarm.id.gt(cursor));

        return jpaQueryFactory.select(
                        Projections.constructor(
                                TicketAlarmDto.class,
                                ticketAlarm.id,
                                ticketAlarm.subscriber.id,
                                ticketAlarm.ticket.id
                        ))
                .from(ticketAlarm)
                .where(condition)
                .orderBy(ticketAlarm.id.asc())
                .limit(limit)
                .fetch();
    }
}
