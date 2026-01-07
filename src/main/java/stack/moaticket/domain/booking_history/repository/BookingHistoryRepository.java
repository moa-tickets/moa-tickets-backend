package stack.moaticket.domain.booking_history.repository;

import stack.moaticket.application.dto.BookingHistoryDto;
import stack.moaticket.domain.payment.type.PaymentState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingHistoryRepository {

    Optional<BookingDetailHeader> findDetailHeader(Long memberId, String orderId);

    List<BookingHistoryDto.SeatInfo> findSeatInfos(Long paymentId);

    record BookingDetailHeader(
            Long paymentId,
            String orderId,

            String concertName,
            String hallName,

            String concertDuration,
            int concertAge,
            LocalDateTime concertStart,
            LocalDateTime concertEnd,

            LocalDateTime sessionDate,
            String concertThumbnail,

            long amount,
            PaymentState paymentState,
            LocalDateTime paidAt,
            LocalDateTime canceledAt
    ) {}
}
