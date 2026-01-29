package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import settings.support.util.TestUtil;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.type.PaymentState;

public class PaymentFixture extends BaseFixture<Payment, Long> {
    private final PaymentRepository paymentRepository;

    public PaymentFixture(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    protected JpaRepository<Payment, Long> repo() {
        return paymentRepository;
    }

    @Transactional
    public Payment create(Member member, long amount) {
        return save(Payment.builder()
                .orderId(TestUtil.uuid())
                .orderName(TestUtil.uniqueString("name"))
                .paymentKey(TestUtil.uuid())
                .amount(amount)
                .state(PaymentState.READY)
                .member(member)
                .build());
    }
}
