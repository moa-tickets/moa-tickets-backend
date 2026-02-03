package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.model.ConfirmContext;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.domain.ticket.dto.TicketHoldDto;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentConfirmValidatorService {
    private final Validator validator;
    private final MemberService memberService;
    private final PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;
    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

    private static final int MAX_TICKETS_PER_SESSION = 4;

    @Transactional
    public ConfirmContext confirmPrepare(Long memberId, PaymentDto.ConfirmRequest request) {
        // 1. 동일 사용자 결제 confirm 직렬화
        memberService.lockById(memberId);

        // 2. member 조회 + 상태 검증
        validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED);

        // 3. request 기본 검증
        if (request == null
                || request.getOrderId() == null || request.getOrderId().isBlank()
                || request.getPaymentKey() == null || request.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        String orderId = request.getOrderId();
        long reqAmount = request.getAmount();

        // 4. payment FOR UPDATE
        Payment payment = validator.of(paymentRepositoryQueryDsl.findByOrderIdForUpdate(orderId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.PAYMENT_NOT_FOUND)
                .validateOrThrow(p -> !p.isOwnedBy(memberId), MoaExceptionType.FORBIDDEN)
                .validateOrThrow(p -> !(p.isConfirmable() || p.isPaid()), MoaExceptionType.PAYMENT_STATE_INVALID)
                .validateOrThrow(p -> !p.isAmountEquals(reqAmount), MoaExceptionType.INVALID_PAYMENT_AMOUNT)
                .get();

        boolean alreadyPaid = payment.isPaid();
        if (alreadyPaid) {
            // 멱등 confirm
            return new ConfirmContext(
                    payment.getId(),
                    memberId,
                    payment.getOrderId(),
                    request.getPaymentKey(),
                    reqAmount,
                    true
            );
        }

        // 5. holdToken → 티켓 조회
        String holdToken = payment.getHoldToken();
        if (holdToken == null || holdToken.isBlank()) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 이전 코드 (OSIV:false)
//        List<Ticket> tickets = ticketRepositoryQueryDsl.findTicketsByHoldToken(holdToken);

        // 수정 코드
        List<TicketHoldDto> tickets = ticketRepositoryQueryDsl.findTicketsDto(holdToken);
        if (tickets.isEmpty()) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 6. hold 유효성 검증
        LocalDateTime now = LocalDateTime.now();

        // 이전 코드 (OSIV:false)
//        boolean allHoldValid = tickets.stream().allMatch(t -> t.isHoldValidAt(now));

        // 수정 코드
        boolean allHoldValid = tickets.stream().allMatch(t -> t.state() == TicketState.HOLD);

        if (!allHoldValid) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 7. quota 체크
        // 이전 코드 (OSIV:false)
//        Long sessionId = tickets.getFirst().getSession().getId();

        // 수정 코드
        Long sessionId = tickets.getFirst().sessionId();
        int toSell = tickets.size();

        long alreadyUsed =
                ticketRepositoryQueryDsl.countByMemberAndSessionAndStates(memberId, sessionId, List.of(TicketState.SOLD, TicketState.PAYMENT_PENDING));


        if (alreadyUsed + toSell > MAX_TICKETS_PER_SESSION) {
            throw new MoaException(MoaExceptionType.TICKET_LIMIT_EXCEEDED);
        }

        // 8. HOLD -> PAYMENT_PENDING (조건부 UPDATE)
        // 이전 코드 (OSIV:false)
//        List<Long> ticketIds = tickets.stream().map(Ticket::getId).toList();

        // 수정 코드
        List<Long> ticketIds = tickets.stream().map(TicketHoldDto::ticketId).toList();

        long updatedRows = ticketRepositoryQueryDsl.updateHoldToPaymentPending(ticketIds, memberId, sessionId, holdToken, now);

        if (updatedRows != ticketIds.size()) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 9. Toss 호출 가능
        return new ConfirmContext(
                payment.getId(),
                memberId,
                payment.getOrderId(),
                request.getPaymentKey(),
                reqAmount,
                false
        );
    }
}
