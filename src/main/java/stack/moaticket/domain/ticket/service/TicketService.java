package stack.moaticket.domain.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.hall.type.HallType;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepository;
import stack.moaticket.domain.ticket.type.TicketState;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    @Transactional
    public void insertTickets(Session session){
        HallType hallType = session.getConcert().getHall().getType();
        List<Ticket> tickets = new ArrayList<>();
        int totalTickets = hallType.getCol() * hallType.getRow();
        for (int i = 1; i <= totalTickets; i++){
            tickets.add(Ticket.builder()
                            .session(session)
                            .num(i)
                            .state(TicketState.AVAILABLE)
                    .build());
        }
        ticketRepository.saveAll(tickets);
    }
}
