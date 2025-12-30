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
@Table(name = "payment")
public class Payment extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "payment_code", nullable = false, updatable = false, unique = true)
    private String code;

    @Column(name = "payment_amount", nullable = false, updatable = false)
    private int amount;

    @Column(name = "payment_state", nullable = false)
    private PaymentState state;

    @Column(name = "payment_paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;
}
