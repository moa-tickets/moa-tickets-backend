package stack.moaticket.domain.ticket_hold.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "ticket_hold")
public class TicketHold extends Base {

    @Id
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "hold_token", nullable = false, length = 100)
    private String holdToken;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
