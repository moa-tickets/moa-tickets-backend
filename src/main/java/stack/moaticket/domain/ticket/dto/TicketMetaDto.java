package stack.moaticket.domain.ticket.dto;

import java.time.LocalDateTime;

public record TicketMetaDto(
        Long ticketId,
        Long sessionId,
        int seatNum,
        LocalDateTime sessionStartTime,
        String concertName
) {}
