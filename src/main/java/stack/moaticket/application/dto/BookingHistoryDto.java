package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import stack.moaticket.domain.payment.type.PaymentState;

import java.time.LocalDateTime;
import java.util.List;

public abstract class BookingHistoryDto {

    @Getter
    @Builder
    public static class DetailResponse {
        // 예매번호
        private String orderId;

        // 콘서트 정보
        private String concertName;
        private String hallName;

        // 공연 기간/정보
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private int concertAge;
        private String concertDuration;       // concert_duration (120분)

        // 관람 회차(관람일시)
        private LocalDateTime sessionDate;

        // 썸네일
        private String concertThumbnail;

        // 예매/결제 정보
        private int ticketCount;
        private long amount;

        private PaymentState paymentState;    // PAID / CANCELED 등
        private LocalDateTime paidAt;
        private LocalDateTime canceledAt;

        // 좌석 정보
        private List<SeatInfo> seats;
    }

    @Getter
    public static class SeatInfo {
        private final Long ticketId;
        private final int seatNum;

        public SeatInfo(Long ticketId, int seatNum) {
            this.ticketId = ticketId;
            this.seatNum = seatNum;
        }
    }
}
