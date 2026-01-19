package stack.moaticket.domain.session_start_alarm.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStartAlarmType {
    LEFT_10("ss_10_", "LEFT_10"),
    ON_HOUR("ss_on_", "ON_HOUR");

    private final String prefix;
    private final String name;
}