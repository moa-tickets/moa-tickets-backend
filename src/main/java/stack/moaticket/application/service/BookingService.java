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
    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl; //TODO: querydsl 수정했으니 확인해주세요

    private static final int HOLD_MINUTES = 10;
    private static final int MAX_TICKETS_PER_HOLD = 4;

    // 좌석 임시 점유 (AVAILABLE -> HOLD)
    @Transactional
    public HoldResult holdTickets(Long memberId, Long sessionId, List<Long> ticketIds) {
        // Member에 락을 걸어 동일 사용자 동시 처리 방지하는 방법도 있는데 쓰면 DB 성능에 영향이 있을지 체크하기
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
        if(alreadyBought + sortedIds.size() > MAX_TICKETS_PER_HOLD) {
            throw new MoaException(MoaExceptionType.TICKET_LIMIT_EXCEEDED);
        }

        // 비관적 락으로 티켓들 조회
        List<Ticket> tickets = ticketRepositoryQueryDsl.findTicketsForUpdate(sortedIds, sessionId);

        // 요청한 개수만큼 전부 조회됐는지(세션/존재 검증)
        if(tickets.size() != sortedIds.size()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        // 모두 AVAILABLE 상태인지 검증
        boolean allAvailable = tickets.stream().allMatch(Ticket::isAvailable);
        if(!allAvailable) {
            throw new MoaException(MoaExceptionType.TICKET_ALREADY_HELD);
        }

        // HOLD 토큰/만료시간 생성
        String holdToken = TokenGenerator.generateHoldToken();
        LocalDateTime expiresAt = now.plusMinutes(HOLD_MINUTES);

        // 엔티티에 hold 정보 세팅 (영속성 컨텍스트(메모리)만 변경)
        for (Ticket t : tickets) {
            t.holdBy(member, holdToken, expiresAt);
        }
        // 트랜잭션 커밋 시점에 JPA flush -> UPDATE (영속성 컨텍스트 → DB 버퍼 풀)

        return new HoldResult(holdToken, expiresAt);
    }

    // 회차별 좌석 목록 조회
    // hold 판단은 ticket_state 기준 (hold -> release 스케줄러가 처리), sold는 ticket_state 기준
    public List<BookingDto.TicketResponse> getTicketsBySession(Long sessionId) {
        if(sessionId == null){
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED, "회차 ID가 전달되지 않았습니다.");
        }

        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsBySession(sessionId);

        return tickets.stream()
                .map(t -> BookingDto.TicketResponse.builder()
                        .ticketId(t.getId())
                        .seatNum(t.getNum())
                        .state(t.viewState().name())
                        .build())
                .toList();
    }

    //TODO: private으로 한 이유는 뭔가요? 테스트를 안하시나요?
    private void validateHoldRequest(Long sessionId, List<Long> ticketIds) {
        if (sessionId == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED, "같은 회차 ID가 전달되지 않았습니다.");
        }
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED, "티켓 ID가 전달되지 않았습니다.");
        }
        if (ticketIds.size() > MAX_TICKETS_PER_HOLD) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED, "한 회차에 최대 4개의 티켓만 예약할 수 있습니다.");
        }
        if (new HashSet<>(ticketIds).size() != ticketIds.size()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED, "중복된 티켓 ID가 포함되어 있습니다.");
        }
    }


    //Service 내부 전용 결과 객체
    public record HoldResult(String holdToken, LocalDateTime expiresAt) {}
}
