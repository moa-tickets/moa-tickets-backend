package stack.moaticket.domain.session_start_alarm.repository;

import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;

import java.util.List;

public interface SessionStartAlarmQueryDslRepository {
    List<SessionStartAlarmMetaDto> getProcessedSessionStartAlarmList(List<Long> alarmIdList);
}
