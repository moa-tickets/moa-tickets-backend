package stack.moaticket.domain.ticket.dto;

import stack.moaticket.domain.ticket.type.TicketState;

import java.time.LocalDateTime;

public record TicketHoldDto(long ticketId, TicketState state, LocalDateTime expiresAt, long sessionId) {}
