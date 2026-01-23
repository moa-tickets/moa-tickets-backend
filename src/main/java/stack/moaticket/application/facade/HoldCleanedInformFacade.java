package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket.service.TicketService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HoldCleanedInformFacade {
    private final TicketService ticketService;

    public List<Long> extractCandidates(LocalDateTime now, Long batchSize) {
        return ticketService.getHoldTicketIdList(now, batchSize);
    }

    @Transactional
    public void release(LocalDateTime now, List<Long> ticketIdList) {
        ticketService.releaseHoldTickets(now, ticketIdList);
    }

    public List<TicketMetaDto> getChanged(List<Long> ticketIdList) {
        return ticketService.getTicketEssentialInfoList(ticketIdList);
    }

}
