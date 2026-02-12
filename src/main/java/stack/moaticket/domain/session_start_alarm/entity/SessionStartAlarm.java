package stack.moaticket.domain.session_start_alarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "session_start_alarm", indexes = {
        @Index(
                name = "session_start_alarm_send",
                columnList = "session_start_alarm_state, session_start_alarm_at, session_start_alarm_id"),
})
public class SessionStartAlarm extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_start_alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private Session session;

    @Column(name = "session_start_alarm_at", nullable = false, updatable = false)
    private LocalDateTime alarmAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_start_alarm_state", nullable = false)
    private SessionStartAlarmState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_start_alarm_type", nullable = false, updatable = false)
    private SessionStartAlarmType type;
}
