package stack.moaticket.system.alarm.core.model;

public record HeartbeatPayload(
        String message
) implements AlarmPayload {}
