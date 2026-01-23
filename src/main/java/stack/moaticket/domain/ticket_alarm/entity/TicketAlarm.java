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
@Table(name = "ticket_alarm", indexes = {
        @Index(name = "idx_ticket_id_member_id", columnList = "ticket_id, member_id")
})
public class TicketAlarm extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;
}
