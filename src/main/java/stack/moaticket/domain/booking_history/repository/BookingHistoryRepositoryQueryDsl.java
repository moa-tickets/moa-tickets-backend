package stack.moaticket.domain.booking_history.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.application.dto.BookingHistoryDto;
import stack.moaticket.domain.payment.type.PaymentState;

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
public class BookingHistoryRepositoryQueryDsl implements BookingHistoryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * Query 1) 상세 헤더 조회 (좌석 제외)
     * - orderId + memberId 조건
     * - 상태: PAID, CANCELED 허용
     * - payment_ticket -> ticket -> session -> concert -> hall
     * - 여러 장이면 row가 여러 개가 될 수 있어 fetchFirst()로 1행만 사용
     */
    @Override
    public Optional<BookingDetailHeader> findDetailHeader(Long memberId, String orderId) {
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

    /**
     * Query 2) 좌석 리스트 조회
     */
    @Override
    public List<BookingHistoryDto.SeatInfo> findSeatInfos(Long paymentId) {
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
}
