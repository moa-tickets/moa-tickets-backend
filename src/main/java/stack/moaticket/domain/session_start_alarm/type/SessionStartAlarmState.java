package stack.moaticket.domain.session_start_alarm.type;

import lombok.Getter;

@Getter
public enum SessionStartAlarmState {
    PENDING, CLAIMED, SENT, DISCONNECTED, CLEANED, SKIPPED
}