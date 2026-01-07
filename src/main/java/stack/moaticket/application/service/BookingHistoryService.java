package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.BookingHistoryDto;
import stack.moaticket.domain.booking_history.repository.BookingHistoryRepository;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingHistoryService {

    private final BookingHistoryRepository bookingHistoryRepository;

    public BookingHistoryDto.DetailResponse getDetail(Long memberId, String orderId) {
        if (memberId == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        if (orderId == null || orderId.isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        BookingHistoryRepository.BookingDetailHeader header = bookingHistoryRepository
                .findDetailHeader(memberId, orderId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.PAYMENT_NOT_FOUND));

        List<BookingHistoryDto.SeatInfo> seats =
                bookingHistoryRepository.findSeatInfos(header.paymentId());

        return BookingHistoryDto.DetailResponse.builder()
                .orderId(header.orderId())

                .concertName(header.concertName())
                .hallName(header.hallName())
                .concertStart(header.concertStart())
                .concertEnd(header.concertEnd())
                .concertAge(header.concertAge())
                .concertDuration(header.concertDuration())
                .sessionDate(header.sessionDate())
                .concertThumbnail(header.concertThumbnail())

                .ticketCount(seats.size())
                .amount(header.amount())
                .paymentState(header.paymentState())
                .paidAt(header.paidAt())
                .canceledAt(header.canceledAt())

                .seats(seats)
                .build();
    }
}
