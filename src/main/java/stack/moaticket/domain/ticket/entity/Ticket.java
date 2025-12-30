package stack.moaticket.domain.ticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.seat.entity.Seat;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.type.TicketState;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "ticket")
public class Ticket extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_state", nullable = false)
    private TicketState state;

    @Column(name = "hold_token", nullable = true)
    private String holdToken;

    @Column(name = "hold_expired", nullable = true)
    private String holdExpired;
}
