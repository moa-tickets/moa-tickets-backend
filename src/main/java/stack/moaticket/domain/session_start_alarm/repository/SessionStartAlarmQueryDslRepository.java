package stack.moaticket.domain.session_start_alarm.repository;

import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionStartAlarmQueryDslRepository {
    List<Long> getSessionStartAlarmIdList(LocalDateTime now, Long batchSize, SessionStartAlarmState state);
    void updatePendingSessionStartAlarmToPassed(LocalDateTime now, List<Long> alarmIdList);
    void updatePendingSessionStartAlarmToProcessed(LocalDateTime now, List<Long> alarmIdList);
    List<SessionStartAlarmMetaDto> getProcessedSessionStartAlarmList(List<Long> alarmIdList);
}
