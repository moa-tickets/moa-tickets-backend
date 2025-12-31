package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;

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

        // 🔒 선택 좌석들 락 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsWithLock(sortedIds);

        if (tickets.size() != sortedIds.size()) {
            throw new IllegalArgumentException("일부 티켓을 찾을 수 없습니다");
        }

        // 모두 같은 session인지 검증
        boolean allSameSession = tickets.stream()
                .allMatch(t -> t.getSession().getId().equals(sessionId));
        if (!allSameSession) {
            throw new IllegalArgumentException("다른 회차의 좌석은 함께 선택할 수 없습니다");
        }

        LocalDateTime now = LocalDateTime.now();

        // 만료된 HOLD는 AVAILABLE로 정리
        tickets.forEach(t -> normalizeExpiredHold(t, now));

        // 모두 AVAILABLE이어야 HOLD 가능
        boolean allAvailable = tickets.stream().allMatch(t -> t.getState() == TicketState.AVAILABLE);
        if (!allAvailable) {
            throw new IllegalStateException("선택한 좌석 중 이미 예약된 좌석이 있습니다");
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

        return new HoldResult(holdToken, sessionId, sortedIds, expiresAt);
    }

    /**
     * 점유 확정 (HOLD -> SOLD)
     * - holdToken만으로 처리
     */
    @Transactional
    public void confirmHold(String holdToken) {
        validateHoldToken(holdToken);

        // 🔒 토큰으로 묶인 티켓들 락 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsByHoldTokenWithLock(holdToken);

        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 holdToken 입니다");
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
            throw new IllegalStateException("점유 정보가 유효하지 않습니다");
        }

        // SOLD 확정 + HOLD 정보 정리(추천)
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
        validateHoldToken(holdToken);

        // 🔒 토큰으로 묶인 티켓들 락 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsByHoldTokenWithLock(holdToken);

        if (tickets.isEmpty()) {
            // idempotent하게 성공 처리할지(OK) / 에러로 할지 선택 가능
            throw new IllegalArgumentException("유효하지 않은 holdToken 입니다");
        }

        LocalDateTime now = LocalDateTime.now();

        // 만료된 HOLD는 AVAILABLE로 정리(이미 풀렸으면 idempotent 처리하고 싶다면 OK)
        tickets.forEach(t -> normalizeExpiredHold(t, now));

        boolean allReleasable = tickets.stream().allMatch(t ->
                // 만료로 이미 풀린 경우까지 허용할지 정책 선택
                t.getState() == TicketState.AVAILABLE
                        || (t.getState() == TicketState.HOLD
                        && holdToken.equals(t.getHoldToken())
                        && t.getHoldExpired() != null
                        && !now.isAfter(t.getHoldExpired()))
        );

        if (!allReleasable) {
            throw new IllegalStateException("점유 해제할 수 없는 좌석이 포함되어 있습니다");
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
            throw new IllegalArgumentException("sessionId는 필수입니다");
        }
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new IllegalArgumentException("ticketIds는 필수입니다");
        }
        if (ticketIds.size() > MAX_TICKETS_PER_HOLD) {
            throw new IllegalArgumentException("최대 " + MAX_TICKETS_PER_HOLD + "개까지 선택 가능합니다");
        }
    }

    private void validateHoldToken(String holdToken) {
        if (holdToken == null || holdToken.isBlank()) {
            throw new IllegalArgumentException("holdToken은 필수입니다");
        }
        // (선택) 형식 검증: "hold_" prefix 등
        // if (!holdToken.startsWith("hold_")) ...
    }

    /**
     * Service 내부 전용 결과 객체 (Controller/DTO는 다음 커밋에서 변환)
     */
    public record HoldResult(String holdToken, Long sessionId, List<Long> ticketIds, LocalDateTime expiresAt) {}
}
