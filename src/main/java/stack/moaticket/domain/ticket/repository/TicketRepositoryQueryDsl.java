package stack.moaticket.domain.ticket.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket.dto.TicketHoldDto;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.type.TicketState;

import java.time.LocalDateTime;
import java.util.List;

import static stack.moaticket.domain.session.entity.QSession.session;
import static stack.moaticket.domain.ticket.entity.QTicket.ticket;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    // 선택 좌석들 비관적 락으로 조회 (FOR UPDATE)
    public List<Ticket> findTicketsForUpdate(List<Long> ticketIds, Long sessionId) {
        if (ticketIds == null || ticketIds.isEmpty()) return List.of();

        List<Long> sortedIds = ticketIds.stream().distinct().sorted().toList();

        return jpaQueryFactory
                .selectFrom(ticket)
                .where(ticket.id.in(sortedIds)
                        .and(ticket.session.id.eq(sessionId)))
                .orderBy(ticket.id.asc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    // 이미 SOLD된 구매 장수 조회
    public long countSoldByMemberAndSession(Long memberId, Long sessionId) {
        if (memberId == null || sessionId == null) return 0;

        Long cnt = jpaQueryFactory
                .select(ticket.id.count())
                .from(ticket)
                .where(ticket.state.eq(TicketState.SOLD)
                        .and(ticket.session.id.eq(sessionId))
                        .and(ticket.member.id.eq(memberId)))
                .fetchOne();

        return cnt == null ? 0 : cnt;
    }

     // SOLD + PAYMENT_PENDING 장수 조회
    public long countByMemberAndSessionAndStates(Long memberId, Long sessionId, List<TicketState> states) {
        Long cnt = jpaQueryFactory
                .select(ticket.id.count())
                .from(ticket)
                .where(ticket.member.id.eq(memberId)
                        .and(ticket.session.id.eq(sessionId))
                        .and(ticket.state.in(states)))
                .fetchOne();

        return cnt == null ? 0L : cnt;
    }

    // HOLD -> PAYMENT_PENDING
    public long updateHoldToPaymentPending(
            List<Long> ticketIds,
            Long memberId,
            Long sessionId,
            String holdToken,
            LocalDateTime now
    ) {
        return jpaQueryFactory
                .update(ticket)
                .set(ticket.state, TicketState.PAYMENT_PENDING)
                .where(ticket.id.in(ticketIds)
                        .and(ticket.state.eq(TicketState.HOLD))
                        .and(ticket.expiresAt.gt(now))
                        .and(ticket.holdToken.eq(holdToken))
                        .and(ticket.member.id.eq(memberId))
                        .and(ticket.session.id.eq(sessionId)))
                .execute();
    }

    // 같은 멤버 + 세션이고 만료되지 않은 hold token이 있는 경우 해제 (만료된 token은 스케줄러가 처리)
    public void releaseActiveHoldsByMemberAndSession(Long memberId, Long sessionId, LocalDateTime now) {
        jpaQueryFactory.update(ticket)
                .set(ticket.state, TicketState.AVAILABLE)
                .set(ticket.holdToken, (String) null)
                .set(ticket.expiresAt, (LocalDateTime) null)
                .set(ticket.member, (Member) null)
                .where(ticket.state.eq(TicketState.HOLD)
                        .and(ticket.session.id.eq(sessionId))
                        .and(ticket.member.id.eq(memberId))
                        .and(ticket.expiresAt.isNotNull())
                        .and(ticket.expiresAt.gt(now))   // not expired
                )
                .execute();
    }

    // 회차별 티켓 목록 조회(좌석 배치도용)
    public List<Ticket> getTicketsBySession(Long sessionId) {
        return jpaQueryFactory.selectFrom(ticket)
                .where(ticket.session.id.eq(sessionId))
                .orderBy(ticket.num.asc())
                .fetch();
    }

    // holdToken으로 티켓 목록 조회(prepare용, 락 없음)
    public List<Ticket> findTicketsByHoldToken(String holdToken) {
        return jpaQueryFactory.selectFrom(ticket)
                .join(ticket.session).fetchJoin()
                .join(ticket.session.concert).fetchJoin()
                .join(ticket.member).fetchJoin()
                .where(ticket.holdToken.eq(holdToken))
                .fetch();
    }

    public List<TicketHoldDto> findTicketsDto(String holdToken) {
        return jpaQueryFactory.select(Projections.constructor(
                TicketHoldDto.class,
                ticket.id,
                ticket.state,
                ticket.expiresAt,
                ticket.session.id
        )).from(ticket).where(ticket.holdToken.eq(holdToken)).fetch();
    }

    // holdToken으로 티켓 목록 조회 + 비관적 락 (confirm 용)
    public List<Ticket> findTicketsByHoldTokenForUpdate(String holdToken) {
        return jpaQueryFactory.selectFrom(ticket)
                .where(ticket.holdToken.eq(holdToken))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    public void releaseHoldByTokenAndMember(String holdToken, Long memberId, LocalDateTime now) {
        if (holdToken == null || holdToken.isBlank() || memberId == null) return;

        jpaQueryFactory.update(ticket)
                .set(ticket.state, TicketState.AVAILABLE)
                .set(ticket.holdToken, (String) null)
                .set(ticket.expiresAt, (LocalDateTime) null)
                .set(ticket.member, (Member) null)
                .where(ticket.holdToken.eq(holdToken)
                        .and(ticket.state.eq(TicketState.HOLD))
                        .and(ticket.member.id.eq(memberId))
                        .and(ticket.expiresAt.isNotNull())
                        .and(ticket.expiresAt.gt(now)) // 아직 유효한 hold만 해제
                )
                .execute();
    }

    public Ticket getTicket(Long ticketId) {
        BooleanExpression condition = ticket.id.eq(ticketId);

        return jpaQueryFactory.selectFrom(ticket)
                .where(condition)
                .fetchFirst();
    }

    public List<TicketMetaDto> getTicketMetaList(List<Long> ticketIdList) {
        BooleanExpression condition = ticket.id.in(ticketIdList)
                .and(ticket.state.eq(TicketState.AVAILABLE))
                .and(ticket.expiresAt.isNull())
                .and(ticket.holdToken.isNull())
                .and(ticket.member.id.isNull());

        return jpaQueryFactory.select(
                Projections.constructor(
                        TicketMetaDto.class,
                        ticket.id,
                        ticket.session.id,
                        ticket.num,
                        ticket.session.date,
                        ticket.session.concert.name
                ))
                .from(ticket)
                .where(condition)
                .orderBy(ticket.id.asc())
                .fetch();
    }

    public List<Ticket> findSoldTicketsByMemberAndSessionForUpdate(Long memberId, Long sessionId) {
        return jpaQueryFactory
                .selectFrom(ticket)
                .join(ticket.session, session)
                .where(
                        ticket.member.id.eq(memberId),
                        ticket.session.id.eq(sessionId),
                        ticket.state.eq(TicketState.SOLD)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE) // FOR UPDATE
                .fetch();
    }

    // 존재 + 세션 정합성 검증용 (락 없음)
    public long countByIdsAndSession(List<Long> ticketIds, Long sessionId) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return 0L;
        }

        Long count = jpaQueryFactory
                .select(ticket.count())
                .from(ticket)
                .where(
                        ticket.id.in(ticketIds)
                                .and(ticket.session.id.eq(sessionId))
                )
                .fetchOne();

        return count != null ? count : 0L;
    }


}
