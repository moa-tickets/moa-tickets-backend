package stack.moaticket.system.alarm.core.model;

public record AlarmEnvelope<P extends AlarmPayload> (
        P payload,
        long triggeredAtMillis
) {}
