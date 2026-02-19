package stack.moaticket.domain.session_start_alarm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.domain.session_start_alarm.repository.SessionStartAlarmRepository;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionStartAlarmService {
    private final SessionStartAlarmRepository sessionStartAlarmRepository;

    public SessionStartAlarm createAndSave(Member member, Session session, LocalDateTime alarmAt, SessionStartAlarmType type) {
        SessionStartAlarm sessionStartAlarm = SessionStartAlarm.builder()
                .session(session)
                .member(member)
                .alarmAt(alarmAt)
                .type(type)
                .state(SessionStartAlarmState.PENDING)
                .build();

        return sessionStartAlarmRepository.save(sessionStartAlarm);
    }

    public List<Long> getPendingSessionAlarmIdList(LocalDateTime now, Long batchSize) {
        return sessionStartAlarmRepository.getSessionStartAlarmIdList(now, batchSize);
    }

    public long updateAlarmIdList(List<Long> alarmIdList, LocalDateTime now) {
        return sessionStartAlarmRepository.updateSessionStartAlarm(alarmIdList, now.minusMinutes(5));
    }

    public List<SessionStartAlarmMetaDto> getProcessedSessionAlarmList(List<Long> alarmIdList) {
        return sessionStartAlarmRepository.getProcessedSessionStartAlarmList(alarmIdList);
    }
}
