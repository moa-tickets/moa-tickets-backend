package stack.moaticket.domain.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepository;
import stack.moaticket.domain.ticket.type.TicketState;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;

    public Ticket createTicket(Session session, int num){
        return Ticket.builder()
                .session(session)
                .num(num)
                .state(TicketState.AVAILABLE)
                .build();
    }

    public void saveAll(List<Ticket> ticketList) {
        ticketRepository.saveAll(ticketList);
    }
}
