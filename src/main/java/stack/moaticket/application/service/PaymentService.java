package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.domain.session.repository.SessionRepositoryQueryDsl;
import stack.moaticket.domain.ticket_hold.entity.TicketHold;
import stack.moaticket.domain.ticket_hold.repository.TicketHoldRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.util.TokenGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final TicketHoldRepositoryQueryDsl ticketHoldRepositoryQueryDsl;
    private final SessionRepositoryQueryDsl sessionRepositoryQueryDsl;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentDto.PrepareResponse prepare(Long memberId, PaymentDto.PrepareRequest request) {
        if (memberId == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        if (request == null || request.getHoldToken() == null || request.getHoldToken().isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        String holdToken = request.getHoldToken();
        LocalDateTime now = LocalDateTime.now();

        // 1) hold 조회
        List<TicketHold> holds = ticketHoldRepositoryQueryDsl.findByHoldToken(holdToken);
        if (holds.isEmpty()) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 2) 만료 검사 (섞여 있으면 토큰 단위로 정리 후 실패)
        boolean expired = holds.stream().anyMatch(h -> !h.getExpiresAt().isAfter(now));
        if (expired) {
            ticketHoldRepositoryQueryDsl.deleteByHoldToken(holdToken);
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 3) 소유자 검사
        boolean owner = holds.stream().allMatch(h -> h.getMember().getId().equals(memberId));
        if (!owner) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        // 4) sessionId 단일성 검사 + price 조회
        Long sessionId = holds.get(0).getSessionId();
        boolean allSameSession = holds.stream().allMatch(h -> h.getSessionId().equals(sessionId));
        if (!allSameSession) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        SessionRepositoryQueryDsl.SessionPaymentInfo info = sessionRepositoryQueryDsl.getPaymentInfo(sessionId);
        if (info == null) {
            throw new MoaException(MoaExceptionType.SESSION_NOT_FOUND);
        }

        long amount = (long) info.price() * holds.size();

        // 5) orderName/orderId 생성
        String orderId = TokenGenerator.generateOrderId();
        String orderName = info.concertName() + " " + holds.size() + "매";

        // 6) Payment(READY) 생성/저장
        Payment payment = Payment.builder()
                .member(memberRepository.getReferenceById(memberId))
                .orderId(orderId)
                .orderName(orderName)
                .holdToken(holdToken)
                .amount(amount)
                .state(PaymentState.READY)
                .build();

        paymentRepository.save(payment);

        return PaymentDto.PrepareResponse.builder()
                .orderId(orderId)
                .orderName(orderName)
                .amount(amount)
                .build();
    }
}
