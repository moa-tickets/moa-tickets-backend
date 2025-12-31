package stack.moaticket.domain.ticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.type.TicketState;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(
        name = "ticket",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_seat_num",
                        columnNames = {"session_id", "seat_num"}
                )
        })
public class Ticket extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "seat_num", nullable = false)
    private int num;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_state", nullable = false)
    private TicketState state;

    @Column(name = "hold_token", nullable = true)
    private String holdToken;

    @Column(name = "hold_expired", nullable = true)
    private LocalDateTime holdExpired;
}
