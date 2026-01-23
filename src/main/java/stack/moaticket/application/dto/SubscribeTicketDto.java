package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public abstract class SubscribeTicketDto {

    @Getter
    public static class Request {
        @NotNull
        private Long ticketId;
    }
}
