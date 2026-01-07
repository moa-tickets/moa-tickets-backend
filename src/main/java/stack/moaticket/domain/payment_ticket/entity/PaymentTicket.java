package stack.moaticket.domain.payment_ticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.ticket.entity.Ticket;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "payment_ticket",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_ticket_payment_ticket", columnNames = {"payment_id", "ticket_id"})
        },
        indexes = {
                @Index(name = "idx_payment_ticket_payment", columnList = "payment_id")
        })
public class PaymentTicket extends Base {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;
}
