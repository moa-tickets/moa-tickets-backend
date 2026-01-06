package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.type.PaymentState;

@Service
@RequiredArgsConstructor
public class PaymentFailWriter {

    private final PaymentRepository paymentRepository;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = Exception.class
    )
    public void markFailed(Long paymentId, String reason) {
        Payment p = paymentRepository.findById(paymentId).orElse(null);
        if (p == null) return; // 이미 삭제되었거나 하면 그냥 무시(안전)

        p.setState(PaymentState.FAILED);
        p.setFailReason(reason);
        paymentRepository.save(p);
    }
}
