package stack.moaticket.domain.session_start_alarm.dto;

import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import java.time.LocalDateTime;

public record SessionStartAlarmMetaDto(
        Long alarmId,
        Long memberId,
        Long sessionId,
        String concertName,
        SessionStartAlarmType type,
        LocalDateTime startAt
) {}
