package stack.moaticket.domain.ticket_alarm.repository;

import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket_alarm.dto.TicketAlarmDto;

import java.util.List;

public interface TicketAlarmQueryDslRepository {
    void delete(Member member, Ticket ticket, Long ticketAlarmId);
    List<TicketAlarmDto> getReceiverDtoList(List<Long> ticketIdList, Long cursor, int limit);
}
