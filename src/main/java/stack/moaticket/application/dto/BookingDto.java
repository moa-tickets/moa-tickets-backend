package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class BookingDto {

    // ===== Request =====
    @Getter
    @NoArgsConstructor
    public static class HoldRequest {
        private Long sessionId;
        private List<Long> ticketIds;
    }

    // ===== Response =====
    // HOLD
    @Getter
    @Builder
    public static class HoldResponse {
        private String holdToken;
        private LocalDateTime expiresAt;
    }

    // 좌석 조회
    @Getter
    @Builder
    public static class TicketResponse {
        private Long ticketId;
        private Integer seatNum;
        private String state; // AVAILABLE/HOLD/SOLD
    }
}
