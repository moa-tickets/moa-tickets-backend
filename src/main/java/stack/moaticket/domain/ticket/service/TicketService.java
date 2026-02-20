package stack.moaticket.domain.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepository;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

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

    public Ticket get(Long ticketId) {
        return ticketRepositoryQueryDsl.getTicket(ticketId);
    }

    public List<Long> getHoldTicketIdList(LocalDateTime now, Long batchSize) {
        return ticketRepository.getHoldTicketIdList(now, batchSize);
    }

    public long releaseHoldTickets(LocalDateTime now, List<Long> ticketIdList) {
        return ticketRepository.releaseHoldTickets(now, ticketIdList);
    }

    public List<TicketMetaDto> getTicketMetadataList(List<Long> ticketIdList) {
        return ticketRepositoryQueryDsl.getTicketMetaList(ticketIdList);
    }
}
