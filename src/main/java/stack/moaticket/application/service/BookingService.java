package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

    private static final int HOLD_MINUTES = 10;
    private static final int MAX_TICKETS_PER_HOLD = 4;

    /**
     * 좌석 임시 점유 (HOLD)
     * - sessionId의 좌석(ticketIds)을 최대 4개까지 한 번에 점유
     * - 락으로 조회하여 동시 점유를 방지
     */
    @Transactional
    public HoldResult holdTickets(Long sessionId, List<Long> ticketIds) {
        validateHoldRequest(sessionId, ticketIds);

        // 데드락 방지: 항상 같은 순서로 락 획득
        List<Long> sortedIds = ticketIds.stream().sorted().toList();

        // 선택 좌석들 락 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsWithLock(sortedIds);

        if (tickets.size() != sortedIds.size()) {
            throw new MoaException(MoaExceptionType.TICKET_NOT_FOUND);
        }

        // 모두 같은 session인지 검증
        boolean allSameSession = tickets.stream()
                .allMatch(t -> t.getSession().getId().equals(sessionId));
        if (!allSameSession) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        LocalDateTime now = LocalDateTime.now();

        // 만료된 HOLD는 AVAILABLE로 정리
        tickets.forEach(t -> normalizeExpiredHold(t, now));

        // 상태별로 명확하게 409 처리
        for (Ticket t : tickets) {
            if (t.getState() == TicketState.SOLD) {
                throw new MoaException(MoaExceptionType.TICKET_ALREADY_SOLD);
            }
            if (t.getState() == TicketState.HOLD) {
                // 만료된 HOLD는 위에서 AVAILABLE로 풀렸으니 여기서 HOLD면 "만료 전 다른 사람이 선점중"
                throw new MoaException(MoaExceptionType.TICKET_ALREADY_HELD);
            }
        }

        // HOLD 토큰/만료시간 생성
        String holdToken = "hold_" + UUID.randomUUID();
        LocalDateTime expiresAt = now.plusMinutes(HOLD_MINUTES);

        // 상태 변경 (dirty checking으로 UPDATE)
        for (Ticket t : tickets) {
            t.setState(TicketState.HOLD);
            t.setHoldToken(holdToken);
            t.setHoldExpired(expiresAt);
        }

        return new HoldResult(holdToken, expiresAt);
    }

    /**
     * 점유 확정 (HOLD -> SOLD)
     * - holdToken만으로 처리
     */
    @Transactional
    public void confirmHold(String holdToken) {
        validateHoldToken(holdToken);

        // 토큰으로 묶인 티켓들 락 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsByHoldTokenWithLock(holdToken);

        if (tickets.isEmpty()) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        LocalDateTime now = LocalDateTime.now();

        // 만료된 HOLD는 AVAILABLE로 정리(이 경우 confirm은 실패해야 함)
        tickets.forEach(t -> normalizeExpiredHold(t, now));

        boolean allHeldByToken = tickets.stream().allMatch(t ->
                t.getState() == TicketState.HOLD
                        && holdToken.equals(t.getHoldToken())
                        && t.getHoldExpired() != null
                        && !now.isAfter(t.getHoldExpired())
        );

        if (!allHeldByToken) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // SOLD 확정 + HOLD 정보 정리
        for (Ticket t : tickets) {
            t.setState(TicketState.SOLD);
            t.setHoldToken(null);
            t.setHoldExpired(null);
        }
    }

    /**
     * 점유 해제 (HOLD -> AVAILABLE)
     * - holdToken만으로 처리
     */
    @Transactional
    public void releaseHold(String holdToken) {
        // 없거나 빈 토큰 그냥 성공 처리
        if (holdToken == null || holdToken.isBlank()) {
            return;
        }

        // 토큰으로 묶인 티켓들 락 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsByHoldTokenWithLock(holdToken);

        // 토큰이 원래 없으면 그냥 성공 처리
        if (tickets.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 만료된 HOLD는 AVAILABLE로 정리
        tickets.forEach(t -> normalizeExpiredHold(t, now));

        // HOLD 토큰/만료가 이상할 경우 토큰 소유가 아니므로 403 처리해야하지만 인증 붙으면 변경
        boolean allReleasable = tickets.stream().allMatch(t ->
                t.getState() == TicketState.AVAILABLE
                        || (t.getState() == TicketState.HOLD
                        && holdToken.equals(t.getHoldToken())
                        && t.getHoldExpired() != null
                        && !now.isAfter(t.getHoldExpired()))
        );

        if (!allReleasable) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        for (Ticket t : tickets) {
            if (t.getState() == TicketState.HOLD) {
                t.setState(TicketState.AVAILABLE);
                t.setHoldToken(null);
                t.setHoldExpired(null);
            }
        }
    }

    private void normalizeExpiredHold(Ticket t, LocalDateTime now) {
        if (t.getState() == TicketState.HOLD
                && t.getHoldExpired() != null
                && now.isAfter(t.getHoldExpired())) {
            t.setState(TicketState.AVAILABLE);
            t.setHoldToken(null);
            t.setHoldExpired(null);
        }
    }

    private void validateHoldRequest(Long sessionId, List<Long> ticketIds) {
        if (sessionId == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (ticketIds.size() > MAX_TICKETS_PER_HOLD) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
    }

    private void validateHoldToken(String holdToken) {
        if (holdToken == null || holdToken.isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

    }

    /**
     * Service 내부 전용 결과 객체
     */
    public record HoldResult(String holdToken, LocalDateTime expiresAt) {}
}
