package stack.moaticket.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.payment.type.PaymentState;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_member", columnList = "member_id"),
        @Index(name = "idx_payment_state", columnList = "payment_state")
})
public class Payment extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Toss 요청, 응답에 사용하는 orderId
    @Column(name = "order_id", nullable = false, updatable = false, unique = true, length = 64)
    private String orderId;

    // Toss paymentKey (confirm 성공 이후 채워짐)
    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "order_name", nullable = false, length = 100)
    private String orderName;

    @Column(name = "hold_token", nullable = false, length = 200)
    private String holdToken;

    @Column(name = "payment_amount", nullable = false, updatable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_state", nullable = false)
    private PaymentState state;

    @Column(name = "payment_paid_at", nullable = true)
    private LocalDateTime paidAt;

    @Column(name = "payment_canceled_at", nullable = true)
    private LocalDateTime canceledAt;

    @Column(name= "fail_reason", nullable = true, length = 300)
    private String failReason;
}
