package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.BookingDto;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.util.TokenGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    private final Validator validator;

    private final MemberService memberService;
    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;


    private static final int HOLD_MINUTES = 10;
    private static final int MAX_TICKETS_PER_HOLD = 4;

    // 좌석 임시 점유 (AVAILABLE -> HOLD)
    @Transactional
    public HoldResult holdTickets(Long memberId, Long sessionId, List<Long> ticketIds) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();
        validateHoldRequest(sessionId, ticketIds);
        LocalDateTime now = LocalDateTime.now();

        // 데드락 방지: 항상 같은 순서로 락 획득
        List<Long> sortedIds = ticketIds.stream().distinct().sorted().toList();

        // 구매 장수 제한(SOLD 기준)
        long alreadyBought = ticketRepositoryQueryDsl.countSoldByMemberAndSession(memberId, sessionId);
        if(alreadyBought + sortedIds.size() > 4) {
            throw new MoaException(MoaExceptionType.TICKET_LIMIT_EXCEEDED);
        }

        // 자동 교체: 같은 멤버+세션 기존 hold 즉시 해제
        // TODO 실제로 남은 시간 vs 같은 멤버가 재접속했을 때 해제하는 정책적 시간 중 min인쪽으로 변경? (아직 반영X)
        ticketRepositoryQueryDsl.releaseActiveHoldsByMemberAndSession(memberId, sessionId, now);

        // 비관적 락으로 티켓들 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.findTicketsForUpdate(sortedIds, sessionId);

        // 요청한 개수만큼 전부 조회됐는지(세션/존재 검증)
        if(tickets.size() != sortedIds.size()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        // 락 잡힌 상태에서 만료 HOLD만 해제
        // TODO 스케줄러로 정리하게 되는 부분?
        for (Ticket t : tickets) {
            if (t.getState() == TicketState.HOLD && t.isHoldExpired(now)) {
                t.clearHold();
            }
        }

        // 모두 AVAILABLE 상태인지 검증
        boolean allAvailable = tickets.stream().allMatch(t -> t.getState() == TicketState.AVAILABLE);
        if(!allAvailable) {
            throw new MoaException(MoaExceptionType.TICKET_ALREADY_HELD);
        }

        // HOLD 토큰/만료시간 생성
        String holdToken = TokenGenerator.generateHoldToken();
        LocalDateTime expiresAt = now.plusMinutes(HOLD_MINUTES);

        // 엔티티에 hold 정보 세팅
        for (Ticket t : tickets) {
            t.setState(TicketState.HOLD);
            t.setHoldToken(holdToken);
            t.setExpiresAt(expiresAt);
            t.setMember(member);
        }
        // 트랜잭션 커밋 시점에 flush -> UPDATE

        return new HoldResult(holdToken, expiresAt);
    }


    // 좌석 점유 해제
    @Transactional
    public void releaseHold(Long memberId, String holdToken) {
        validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED);
        if (holdToken == null || holdToken.isBlank()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        ticketRepositoryQueryDsl.releaseHoldByTokenAndMember(holdToken, memberId, now);
    }

    // 회차별 좌석 목록 조회
    // hold 판단은 ticket_hold(expires_at > now) 존재 여부로 처리
    // sold는 ticket_state 기준
    public List<BookingDto.TicketResponse> getTicketsBySession(Long sessionId) {
        LocalDateTime now = LocalDateTime.now();

        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsBySession(sessionId);

        return tickets.stream()
                .map(t -> BookingDto.TicketResponse.builder()
                        .ticketId(t.getId())
                        .seatNum(t.getNum())
                        .state(resolveStateForView(t, now))
                        .build())
                .toList();
    }

    private String resolveStateForView(Ticket ticket, LocalDateTime now) {
        if (ticket.getState() == TicketState.SOLD) {
            return TicketState.SOLD.name();
        }

        if(ticket.getState() == TicketState.HOLD){
            return ticket.isHoldExpired(now) ? TicketState.AVAILABLE.name() : TicketState.HOLD.name();
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


    //Service 내부 전용 결과 객체
    public record HoldResult(String holdToken, LocalDateTime expiresAt) {}
}
