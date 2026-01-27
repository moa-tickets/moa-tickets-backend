package stack.moaticket.domain.ticket_alarm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket_alarm.dto.TicketAlarmDto;
import stack.moaticket.domain.ticket_alarm.entity.TicketAlarm;
import stack.moaticket.domain.ticket_alarm.repository.TicketAlarmRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketAlarmService {
    private final TicketAlarmRepository ticketAlarmRepository;

    public void createAndSave(Member member, Ticket ticket) {
        TicketAlarm ticketAlarm = TicketAlarm.builder()
                .subscriber(member)
                .ticket(ticket)
                .build();

        ticketAlarmRepository.save(ticketAlarm);
    }

    public void delete(Member member, Ticket ticket) {
        ticketAlarmRepository.delete(member, ticket);
    }

    public List<TicketAlarmDto> getDtoList(List<Long> ticketIdList, Long cursor, int limit) {
        return ticketAlarmRepository.getReceiverDtoList(ticketIdList, cursor, limit);
    }
}
