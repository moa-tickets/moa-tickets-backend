package stack.moaticket.domain.payment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.payment.entity.Payment;

import static stack.moaticket.domain.payment.entity.QPayment.payment;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryQueryDsl {

    private final JPAQueryFactory queryFactory;

    public Payment findByOrderId(String orderId) {
        return queryFactory
                .selectFrom(payment)
                .where(payment.orderId.eq(orderId))
                .fetchOne();
    }
}
