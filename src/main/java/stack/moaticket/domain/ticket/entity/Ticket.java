package stack.moaticket.domain.ticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;
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
        },
        indexes = {
                @Index(name = "idx_ticket_hold_token", columnList = "hold_token"),
                @Index(name = "idx_ticket_hold_member", columnList = "member_id"),
                @Index(name = "idx_ticket_hold_expires", columnList = "expires_at"),
                @Index(name = "idx_ticket_session_state", columnList = "session_id, ticket_state")
        })
public class Ticket extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "seat_num", nullable = false)
    private int num;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_state", nullable = false)
    private TicketState state;

    @Column(name = "hold_token", length = 200)
    private String holdToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public boolean isAvailable() {
        return this.state == TicketState.AVAILABLE;
    }

    public boolean isHold() {
        return this.state == TicketState.HOLD;
    }

    public boolean isSold() {
        return this.state == TicketState.SOLD;
    }

    public TicketState viewState() {
        if(isAvailable()) return TicketState.AVAILABLE;
        else if(isHold()) return TicketState.HOLD;
        else return TicketState.SOLD;
    }

    public void holdBy(Member member, String holdToken, LocalDateTime expiresAt){
        this.state = TicketState.HOLD;
        this.member = member;
        this.holdToken = holdToken;
        this.expiresAt = expiresAt;
    }

    public void clearHold() {
        this.holdToken = null;
        this.expiresAt = null;
        this.member = null;
        this.state = TicketState.AVAILABLE;
    }
}
