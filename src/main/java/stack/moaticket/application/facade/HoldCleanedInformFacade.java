package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket.service.TicketService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HoldCleanedInformFacade {
    private final TicketService ticketService;

    @Transactional
    public List<Long> release(LocalDateTime now, Long batchSize) {
        List<Long> ticketIdList = ticketService.getHoldTicketIdList(now, batchSize);
        ticketService.releaseHoldTickets(now, ticketIdList);

        return ticketIdList;
    }

    public Map<Long, TicketMetaDto> getChanged(List<Long> ticketIdList) {
        return ticketService.getTicketMetadataList(ticketIdList)
                .stream()
                .collect(Collectors.toMap(
                        TicketMetaDto::ticketId,
                        Function.identity()
                ));
    }

}
