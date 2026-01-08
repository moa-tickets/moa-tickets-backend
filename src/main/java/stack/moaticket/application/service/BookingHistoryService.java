package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.BookingHistoryDto;
import stack.moaticket.domain.booking_history.repository.BookingHistoryRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingHistoryService {

    private static final int PAGE_SIZE = 10;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BookingHistoryRepositoryQueryDsl bookingHistoryQueryDsl;

    // =========================
    // 상세 조회
    // =========================
    public BookingHistoryDto.DetailResponse getDetail(Long memberId, String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        BookingHistoryRepositoryQueryDsl.BookingDetailHeader header = bookingHistoryQueryDsl
                .getDetailHeader(memberId, orderId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.PAYMENT_NOT_FOUND));

        List<BookingHistoryDto.SeatInfo> seats = bookingHistoryQueryDsl.getSeatInfos(header.paymentId());

        return BookingHistoryDto.DetailResponse.builder()
                .orderId(header.orderId())

                .concertName(header.concertName())
                .hallName(header.hallName())

                .concertDuration(header.concertDuration())
                .concertAge(header.concertAge())
                .concertStart(header.concertStart())
                .concertEnd(header.concertEnd())

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

    // =========================
    // 목록 조회 (offset pagination)
    // - range: D15/M1/M2/M3 (예매일=paidAt 기준)
    // - month filter: basis + year + month
    // =========================
    public BookingHistoryDto.ListResponse getList(
            Long memberId,
            int page,
            BookingHistoryDto.RangeFilter range,
            BookingHistoryDto.MonthBasis basis,
            Integer year,
            Integer month
    ) {

        if (page < 0) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        // range와 month 필터는 동시에 사용하지 않는다고 가정(요구사항 상)
        boolean hasRange = (range != null);
        boolean hasMonth = (basis != null || year != null || month != null);

        if (hasRange && hasMonth) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        // month 필터가 오면 필수값 체크
        if (hasMonth) {
            if (basis == null || year == null || month == null) {
                throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
            }
            if (month < 1 || month > 12) {
                throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
            }
        }

        int offset = page * PAGE_SIZE;
        int limit = PAGE_SIZE;

        // 1) 기간 필터 계산(예매일 기준)
        LocalDateTime bookedFrom = null;
        if (range != null) {
            LocalDateTime now = LocalDateTime.now(KST);
            bookedFrom = switch (range) {
                case D15 -> now.minusDays(15);
                case M1 -> now.minusMonths(1);
                case M2 -> now.minusMonths(2);
                case M3 -> now.minusMonths(3);
            };
        }

        // 2) 월별 필터 계산: [monthStart, monthEnd)
        LocalDateTime monthStart = null;
        LocalDateTime monthEnd = null;
        if (hasMonth) {
            LocalDate firstDay = LocalDate.of(year, month, 1);
            monthStart = LocalDateTime.of(firstDay, LocalTime.MIN); // 00:00
            monthEnd = LocalDateTime.of(firstDay.plusMonths(1), LocalTime.MIN);
        }

        // 3) count + list
        long total = bookingHistoryQueryDsl.countBookingList(
                memberId,
                bookedFrom,
                basis,
                monthStart,
                monthEnd
        );

        List<BookingHistoryRepositoryQueryDsl.BookingListRow> rows = bookingHistoryQueryDsl.getBookingList(
                memberId,
                bookedFrom,
                basis,
                monthStart,
                monthEnd,
                offset,
                limit
        );

        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);

        return BookingHistoryDto.ListResponse.builder()
                .page(page)
                .size(PAGE_SIZE)
                .totalElements(total)
                .totalPages(totalPages)
                .items(
                        rows.stream()
                                .map(r -> BookingHistoryDto.Item.builder()
                                        .orderId(r.orderId())
                                        .concertName(r.concertName())
                                        .sessionDate(r.sessionDate())
                                        .ticketCount((int) r.ticketCount())
                                        .build()
                                )
                                .toList()
                )
                .build();
    }
}
