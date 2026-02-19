package stack.moaticket.application.model;

import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.system.redis.model.RedisValue;

import java.util.List;
import java.util.Map;

public record TicketReleaseRunValue(
        List<Long> ticketIdList,
        Map<Long, TicketMetaDto> metadata
) implements RedisValue {}
