package stack.moaticket.system.alarm.core.model;

import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;

public record SessionStartPayload(
        SessionStartAlarmMetaDto alarm
)
implements AlarmPayload {}
