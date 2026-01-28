package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.domain.session_start_alarm.repository.SessionStartAlarmRepository;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import java.time.LocalDateTime;

public class SessionStartAlarmFixture extends BaseFixture<SessionStartAlarm, Long> {
    private final SessionStartAlarmRepository sessionStartAlarmRepository;

    public SessionStartAlarmFixture(SessionStartAlarmRepository sessionStartAlarmRepository) {
        this.sessionStartAlarmRepository = sessionStartAlarmRepository;
    }

    @Override
    protected JpaRepository<SessionStartAlarm, Long> repo() {
        return sessionStartAlarmRepository;
    }

    @Transactional
    public SessionStartAlarm create(Member member, Session session, LocalDateTime alarmAt, SessionStartAlarmType type) {
        return save(SessionStartAlarm.builder()
                .alarmAt(alarmAt)
                .state(SessionStartAlarmState.PENDING)
                .type(type)
                .member(member)
                .session(session)
                .build());
    }
}
