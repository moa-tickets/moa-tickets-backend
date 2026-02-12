package stack.moaticket.system.alarm.core.model;

public record ConnectPayload(
        String connectionId
) implements AlarmPayload {}
