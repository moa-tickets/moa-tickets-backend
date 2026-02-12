package stack.moaticket.system.alarm.core.model;

import stack.moaticket.domain.ticket.dto.TicketMetaDto;

import java.util.List;

public record TicketReleasePayload(
        List<TicketMetaDto> metaList
)
implements AlarmPayload {}
