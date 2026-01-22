package stack.moaticket.domain.payment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.type.PaymentState;

import static stack.moaticket.domain.payment.entity.QPayment.payment;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryQueryDsl {

    private final JPAQueryFactory queryFactory;

    public Payment findByOrderIdForUpdate(String orderId){
        return queryFactory
                .selectFrom(payment)
                .where(payment.orderId.eq(orderId).and(payment.state.eq(PaymentState.READY)))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();
    }

    public Payment findByOrderId(String orderId){
        return queryFactory
                .selectFrom(payment)
                .where(payment.orderId.eq(orderId))
                .fetchOne();
    }
}
