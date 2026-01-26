package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.model.ConfirmContext;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentConfirmValidatorService {
    private final Validator validator;
    private final MemberService memberService;
    private final PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;

    @Transactional
    public ConfirmContext validateAndLockPayment(Long memberId, PaymentDto.ConfirmRequest request) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        if (request == null
                || request.getOrderId() == null || request.getOrderId().isBlank()
                || request.getPaymentKey() == null || request.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        String orderId = request.getOrderId();
        String paymentKey = request.getPaymentKey();
        long reqAmount = request.getAmount();

        if (reqAmount <= 0) throw new MoaException(MoaExceptionType.VALIDATION_FAILED);

        Payment payment = validator.of(paymentRepositoryQueryDsl.findByOrderIdForUpdate(orderId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.PAYMENT_NOT_FOUND)
                .validateOrThrow(p -> !p.isOwnedBy(memberId), MoaExceptionType.FORBIDDEN)
                .validateOrThrow(p -> !p.isConfirmable(), MoaExceptionType.PAYMENT_STATE_INVALID)
                .validateOrThrow(p -> !p.isAmountEquals(reqAmount), MoaExceptionType.INVALID_PAYMENT_AMOUNT)
                .get();

        boolean alreadyPaid = payment.isPaid();

        return new ConfirmContext(payment.getId(), member.getId(), payment.getOrderId(), paymentKey, reqAmount, alreadyPaid);
    }
}
