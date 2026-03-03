package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.component.gauge.TicketReleaseExecutorGaugeManager;
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
    private final TicketReleaseExecutorGaugeManager manager;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Long> release(LocalDateTime now, Long batchSize) {
        List<Long> ticketIdList = ticketService.getHoldTicketIdList(now, batchSize);
        manager.recordDatabase(() -> ticketService.releaseHoldTickets(now, ticketIdList));

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
