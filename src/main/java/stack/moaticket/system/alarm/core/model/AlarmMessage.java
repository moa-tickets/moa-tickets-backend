package stack.moaticket.system.alarm.core.model;

import lombok.Getter;

@Getter
public final class AlarmMessage<P extends AlarmPayload> {
    private final String key;
    private final AlarmEnvelopeTemplate<P> payload;

    public AlarmMessage(String key, P payload) {
        this.key = key;
        this.payload = new AlarmEnvelopeTemplate<>(payload);
    }
}
