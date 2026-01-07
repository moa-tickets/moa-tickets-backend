package stack.moaticket.domain.booking_history.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.application.dto.BookingHistoryDto;
import stack.moaticket.domain.payment.type.PaymentState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static stack.moaticket.domain.concert.entity.QConcert.concert;
import static stack.moaticket.domain.hall.entity.QHall.hall;
import static stack.moaticket.domain.payment.entity.QPayment.payment;
import static stack.moaticket.domain.payment_ticket.entity.QPaymentTicket.paymentTicket;
import static stack.moaticket.domain.session.entity.QSession.session;
import static stack.moaticket.domain.ticket.entity.QTicket.ticket;

@Repository
@RequiredArgsConstructor
public class BookingHistoryRepositoryQueryDsl {

    private final JPAQueryFactory queryFactory;

    // ===== 상세용 projection =====
    public record BookingDetailHeader(
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

    // ===== 목록용 projection =====
    public record BookingListRow(
            String orderId,
            String concertName,
            LocalDateTime sessionDate,
            long ticketCount
    ) {}

    // =========================
    // 상세 조회 (PAID + CANCELED)
    //     * - orderId + memberId 조건
    //     * - 상태: PAID, CANCELED 허용
    //     * - payment_ticket -> ticket -> session -> concert -> hall
    //     * - 여러 장이면 row가 여러 개가 될 수 있어 fetchFirst()로 1행만 사용
    // =========================
    public Optional<BookingDetailHeader> getDetailHeader(Long memberId, String orderId) {
        BookingDetailHeader result = queryFactory
                .select(Projections.constructor(
                        BookingDetailHeader.class,
                        payment.id,
                        payment.orderId,

                        concert.name,
                        hall.name,

                        concert.duration,
                        concert.age,
                        concert.start,
                        concert.end,

                        session.date,
                        concert.thumbnail,

                        payment.amount,
                        payment.state,
                        payment.paidAt,
                        payment.canceledAt
                ))
                .from(payment)
                .join(paymentTicket).on(paymentTicket.payment.eq(payment))
                .join(paymentTicket.ticket, ticket)
                .join(ticket.session, session)
                .join(session.concert, concert)
                .leftJoin(concert.hall, hall)
                .where(
                        payment.orderId.eq(orderId),
                        payment.member.id.eq(memberId),
                        payment.state.in(PaymentState.PAID, PaymentState.CANCELED)
                )
                .orderBy(ticket.id.asc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    public List<BookingHistoryDto.SeatInfo> getSeatInfos(Long paymentId) {
        return queryFactory
                .select(Projections.constructor(
                        BookingHistoryDto.SeatInfo.class,
                        ticket.id,
                        ticket.num
                ))
                .from(paymentTicket)
                .join(paymentTicket.ticket, ticket)
                .where(paymentTicket.payment.id.eq(paymentId))
                .orderBy(ticket.num.asc())
                .fetch();
    }

    // =========================
    // 목록 조회 (offset)
    // - bookedFrom: 기간 필터(예매일 기준). 없으면 null
    // - monthStart/monthEnd + basis: 월별 필터. 없으면 null
    // =========================
    public List<BookingListRow> getBookingList(
            Long memberId,
            LocalDateTime bookedFrom,
            BookingHistoryDto.MonthBasis basis,
            LocalDateTime monthStart,
            LocalDateTime monthEnd,
            int offset,
            int limit
    ) {
        BooleanBuilder where = baseWhere(memberId);
        applyFilters(where, bookedFrom, basis, monthStart, monthEnd);

        return queryFactory
                .select(Projections.constructor(
                        BookingListRow.class,
                        payment.orderId,
                        concert.name,
                        session.date.min(),
                        paymentTicket.id.count()
                ))
                .from(payment)
                .join(paymentTicket).on(paymentTicket.payment.eq(payment))
                .join(paymentTicket.ticket, ticket)
                .join(ticket.session, session)
                .join(session.concert, concert)
                .where(where)
                .groupBy(payment.id, payment.orderId, concert.name)
                .orderBy(payment.paidAt.desc(), payment.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long countBookingList(
            Long memberId,
            LocalDateTime bookedFrom,
            BookingHistoryDto.MonthBasis basis,
            LocalDateTime monthStart,
            LocalDateTime monthEnd
    ) {
        BooleanBuilder where = baseWhere(memberId);
        applyFilters(where, bookedFrom, basis, monthStart, monthEnd);

        Long cnt = queryFactory
                .select(payment.id.countDistinct())
                .from(payment)
                .join(paymentTicket).on(paymentTicket.payment.eq(payment))
                .join(paymentTicket.ticket, ticket)
                .join(ticket.session, session)
                .where(where)
                .fetchOne();

        return cnt == null ? 0L : cnt;
    }

    private BooleanBuilder baseWhere(Long memberId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(payment.member.id.eq(memberId));
        where.and(payment.state.in(PaymentState.PAID, PaymentState.CANCELED));
        return where;
    }

    private void applyFilters(
            BooleanBuilder where,
            LocalDateTime bookedFrom,
            BookingHistoryDto.MonthBasis basis,
            LocalDateTime monthStart,
            LocalDateTime monthEnd
    ) {
        if (bookedFrom != null) {
            where.and(payment.paidAt.goe(bookedFrom));
        }

        if (basis != null && monthStart != null && monthEnd != null) {
            if (basis == BookingHistoryDto.MonthBasis.BOOKED_AT) {
                where.and(payment.paidAt.goe(monthStart));
                where.and(payment.paidAt.lt(monthEnd));
            } else if (basis == BookingHistoryDto.MonthBasis.VIEWED_AT) {
                where.and(session.date.goe(monthStart));
                where.and(session.date.lt(monthEnd));
            }
        }
    }
}
