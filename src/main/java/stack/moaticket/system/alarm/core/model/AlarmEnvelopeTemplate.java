package stack.moaticket.system.alarm.core.model;

import java.util.function.LongSupplier;

public record AlarmEnvelopeTemplate<P extends AlarmPayload> (
        P payload,
        LongSupplier triggeredAtSupplier
) {
    public AlarmEnvelopeTemplate(P payload) {
        this(payload, System::currentTimeMillis);
    }

    public AlarmEnvelope<P> materialize() {
        return new AlarmEnvelope<>(payload, triggeredAtSupplier.getAsLong());
    }
}
