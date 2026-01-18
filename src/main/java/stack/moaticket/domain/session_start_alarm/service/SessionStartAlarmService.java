package stack.moaticket.domain.session_start_alarm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.domain.session_start_alarm.repository.SessionStartAlarmRepository;
import stack.moaticket.domain.session_start_alarm.repository.SessionStartAlarmRepositoryQueryDsl;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionStartAlarmService {
    private final SessionStartAlarmRepository sessionStartAlarmRepository;
    private final SessionStartAlarmRepositoryQueryDsl sessionStartAlarmRepositoryQueryDsl;

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

    public List<SessionStartAlarm> getClaimedSessionAlarmList(LocalDateTime now) {
        return sessionStartAlarmRepositoryQueryDsl.getClaimedSessionStartAlarmList(now);
    }

    public List<SessionStartAlarm> getPendingSessionAlarmList(Long batchSize, LocalDateTime now) {
        return sessionStartAlarmRepositoryQueryDsl.getSessionStartAlarmList(batchSize, SessionStartAlarmState.PENDING, now);
    }

    public void updatePendingToSkipped(List<SessionStartAlarm> candidateList, LocalDateTime now) {
        sessionStartAlarmRepositoryQueryDsl.updatePendingSessionStartAlarmToSkipped(candidateList, now);
    }

    public void updatePendingToClaimed(List<SessionStartAlarm> candidateList, LocalDateTime now) {
        sessionStartAlarmRepositoryQueryDsl.updatePendingSessionStartAlarmToClaimed(candidateList, now);
    }

    public void cleanupTerminatedClaimedAlarm(LocalDateTime now) {
        sessionStartAlarmRepositoryQueryDsl.updateClaimedSessionStartAlarmToError(now);
    }

    public void updateClaimedToSent(List<SessionStartAlarm> succeededList) {
        sessionStartAlarmRepositoryQueryDsl.updateClaimedSessionStartAlarmToSent(succeededList);
    }

    public void updateClaimedToPendingOrDisconnected(List<SessionStartAlarm> failedList) {
        sessionStartAlarmRepositoryQueryDsl.updateClaimedSessionStartAlarmToDisconnected(failedList);
    }
}
