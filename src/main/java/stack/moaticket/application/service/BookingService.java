package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.BookingDto;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.domain.ticket_hold.entity.TicketHold;
import stack.moaticket.domain.ticket_hold.repository.TicketHoldCommand;
import stack.moaticket.domain.ticket_hold.repository.TicketHoldRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.util.TokenGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;
    private final TicketHoldRepositoryQueryDsl ticketHoldRepositoryQueryDsl;
    private final TicketHoldCommand ticketHoldCommand;
    private final MemberRepository memberRepository;

    private static final int HOLD_MINUTES = 10;
    private static final int MAX_TICKETS_PER_HOLD = 4;


    // 좌석 임시 점유 (최대 4개)
    @Transactional
    public HoldResult holdTickets(Long memberId, Long sessionId, List<Long> ticketIds) {
        validateHoldRequest(sessionId, ticketIds);

        if(memberId == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }

        LocalDateTime now = LocalDateTime.now();

        // 변이 요청에서만 만료 정리(요청 기반 정책 유지)
        ticketHoldRepositoryQueryDsl.deleteExpired(now);

        // 자동 교체: 같은 멤버+세션 기존 hold 즉시 해제
        ticketHoldRepositoryQueryDsl.deleteByMemberAndSession(memberId, sessionId);

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

        // 상태별로 명확하게 409 처리
        for (Ticket t : tickets) {
            if (t.getState() == TicketState.SOLD) {
                throw new MoaException(MoaExceptionType.TICKET_ALREADY_SOLD);
            }
        }

        // HOLD 토큰/만료시간 생성
        String holdToken = TokenGenerator.generateHoldToken();
        LocalDateTime expiresAt = now.plusMinutes(HOLD_MINUTES);

        Member memberRef = memberRepository.getReferenceById(memberId);

        List<TicketHold> holds = tickets.stream()
                .map(t -> TicketHold.builder()
                        .ticket(t)
                        .holdToken(holdToken)
                        .member(memberRef)
                        .sessionId(sessionId)
                        .expiresAt(expiresAt)
                        .build()
                )
                .collect(Collectors.toList());

        try {
            ticketHoldCommand.insertAll(holds);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new MoaException(MoaExceptionType.TICKET_ALREADY_HELD);
        }

        return new HoldResult(holdToken, expiresAt);
    }

    // 점유 확정 (SOLD)
    // 결제 전 임시로 SOLD 처리, 소유자(memberId) 검증
//    @Transactional
//    public void confirmHold(Long memberId, String holdToken) {
//        validateHoldToken(holdToken);
//
//        if (memberId == null) {
//            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
//        }
//
//        LocalDateTime now = LocalDateTime.now();
//
//        // hold 조회
//        List<TicketHold> holds = ticketHoldRepositoryQueryDsl.findByHoldToken(holdToken);
//
//        if (holds.isEmpty()) {
//            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
//        }
//
//        // 만료 처리 : 만료된 게 섞여 있으면 토큰 단위로 정리하고 실패
//        boolean expired = holds.stream().anyMatch(h -> !h.getExpiresAt().isAfter(now));
//        if (expired) {
//            ticketHoldRepositoryQueryDsl.deleteByHoldToken(holdToken);
//            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
//        }
//
//        // 소유자 검증
//        boolean owner = holds.stream().allMatch(h -> h.getMember().getId().equals(memberId));
//        if (!owner) {
//            throw new MoaException(MoaExceptionType.FORBIDDEN);
//        }
//
//        // 해당 ticket들을 락 잡고 SOLD 처리
//        List<Long> ticketIds = holds.stream()
//                .map(TicketHold::getId)
//                .sorted()
//                .toList();
//
//        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsWithLock(ticketIds);
//
//        for (Ticket t : tickets) {
//            if (t.getState() == TicketState.SOLD) {
//                throw new MoaException(MoaExceptionType.TICKET_ALREADY_SOLD);
//            }
//        }
//
//        for (Ticket t : tickets) {
//            t.setState(TicketState.SOLD);
//        }
//
//        // hold 제거
//        ticketHoldRepositoryQueryDsl.deleteByHoldToken(holdToken);
//
//    }


    // 좌석 점유 해제
    // 이미 만료되었을 경우 성공처리(200), 소유자(memberId) 검증
    @Transactional
    public void releaseHold(Long memberId, String holdToken) {
        // 없거나 빈 토큰 그냥 성공 처리
        if (holdToken == null || holdToken.isBlank()) {
            return;
        }

        if (memberId == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }

        LocalDateTime now = LocalDateTime.now();

        List<TicketHold> holds = ticketHoldRepositoryQueryDsl.findByHoldToken(holdToken);

        // 토큰이 원래 없으면 그냥 성공 처리
        if (holds.isEmpty()) {
            return;
        }

        // 만료면 그냥 삭제하고 성공
        boolean expired = holds.stream().anyMatch(h -> !h.getExpiresAt().isAfter(now));
        if (expired) {
            ticketHoldRepositoryQueryDsl.deleteByHoldToken(holdToken);
            return;
        }

        // 소유자 검증
        boolean owner = holds.stream().allMatch(h -> h.getMember().getId().equals(memberId));
        if (!owner) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        // 해제: hold row 삭제만 하면 됨
        ticketHoldRepositoryQueryDsl.deleteByHoldToken(holdToken);

    }

    // 회차별 좌석 목록 조회
    // hold 판단은 ticket_hold(expires_at > now) 존재 여부로 처리
    // sold는 ticket_state 기준
    public List<BookingDto.TicketResponse> getTicketsBySession(Long sessionId) {
        LocalDateTime now = LocalDateTime.now();

        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsBySession(sessionId);
        List<Long> heldTicketIds = ticketHoldRepositoryQueryDsl.findHeldTicketIdsBySession(sessionId, now);
        Set<Long> heldSet = new HashSet<>(heldTicketIds);

        return tickets.stream()
                .map(t -> BookingDto.TicketResponse.builder()
                        .ticketId(t.getId())
                        .seatNum(t.getNum())
                        .state(resolveStateForView(t, heldSet))
                        .build())
                .toList();
    }

    private String resolveStateForView(Ticket ticket, Set<Long> heldTicketIds) {
        if (ticket.getState() == TicketState.SOLD) {
            return TicketState.SOLD.name();
        }
        if (heldTicketIds.contains(ticket.getId())) {
            return TicketState.HOLD.name();
        }
        return TicketState.AVAILABLE.name();
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
        if (new HashSet<>(ticketIds).size() != ticketIds.size()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED, "중복된 티켓 ID가 포함되어 있습니다.");
        }
    }

    private void validateHoldToken(String holdToken) {
        if (holdToken == null || holdToken.isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

    }

    //Service 내부 전용 결과 객체
    public record HoldResult(String holdToken, LocalDateTime expiresAt) {}
}
