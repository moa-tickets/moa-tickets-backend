package stack.moaticket.domain.ticket_alarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.ticket.entity.Ticket;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(
        name = "ticket_alarm",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ticket_id_member_id",
                        columnNames = {"ticket_id", "member_id"}
                )
        })
public class TicketAlarm extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;
}
