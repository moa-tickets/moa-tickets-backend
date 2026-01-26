package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.model.ConfirmContext;
import stack.moaticket.application.service.PaymentConfirmValidatorService;
import stack.moaticket.application.service.PaymentFinalizeService;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.toss.dto.TossConfirmResponse;
import stack.moaticket.system.toss.facade.TossPaymentsFacade;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentConfirmFacade {

    private final PaymentConfirmValidatorService validatorService;
    private final TossPaymentsFacade tossPaymentsFacade;
    private final PaymentFinalizeService paymentFinalizeService;
    private final PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;

    public PaymentDto.ConfirmResponse confirm(Long memberId, PaymentDto.ConfirmRequest request) {
        // 1) TX1 : 검증 + payment 락 조회
        ConfirmContext ctx = validatorService.validateAndLockPayment(memberId, request);

        // 2) TX 밖 : Toss API 호출
        TossConfirmResponse tossResponse = tossPaymentsFacade.confirm(ctx.paymentKey(), ctx.orderId(), ctx.amount());

        if(tossResponse == null || tossResponse.getPaymentKey() == null || tossResponse.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
        }

        // 3) TX2 (REQUIRES_NEW) : 확정 finalize
        paymentFinalizeService.finalizeAfterTossPaid(ctx.paymentId(), tossResponse.getPaymentKey(), memberId, LocalDateTime.now());

        // 4) 결과 조회 (payment를 다시 조회해서 최신 상태로)
        Payment paid = paymentRepositoryQueryDsl.findByOrderId(ctx.orderId());
        return PaymentDto.ConfirmResponse.builder()
                .paymentId(paid.getId())
                .orderId(paid.getOrderId())
                .paymentState(paid.getState())
                .paidAt(paid.getPaidAt())
                .amount(paid.getAmount())
                .orderName(paid.getOrderName())
                .build();

    }
}
