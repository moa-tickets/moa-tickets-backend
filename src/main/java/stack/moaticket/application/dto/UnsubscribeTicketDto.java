package stack.moaticket.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public abstract class UnsubscribeTicketDto {

    @Getter
    public static class Request {
        @NotNull
        private Long ticketId;
        @NotNull
        private Long ticketAlarmId;
    }
}
