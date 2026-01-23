package stack.moaticket.domain.ticket_alarm.repository;

import stack.moaticket.domain.ticket_alarm.dto.TicketAlarmDto;

import java.util.List;

public interface TicketAlarmQueryDslRepository {
    List<TicketAlarmDto> getReceiverDtoList(List<Long> ticketIdList, Long cursor, int limit);
}
