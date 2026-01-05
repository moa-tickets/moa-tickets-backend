package stack.moaticket.domain.ticket_hold.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.ticket.entity.Ticket;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "ticket_hold",
        indexes = {
                @Index(name = "idx_ticket_hold_token", columnList = "hold_token"),
                @Index(name = "idx_ticket_hold_member", columnList = "member_id"),
                @Index(name = "idx_ticket_hold_expires", columnList = "expires_at")
        }
)
public class TicketHold extends Base {

    @Id
    @Column(name = "ticket_id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "ticket_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_hold_ticket"))
    private Ticket ticket;

    @Column(name = "hold_token", nullable = false, length = 200)
    private String holdToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
