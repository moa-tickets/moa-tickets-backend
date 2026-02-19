package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.BasicKey;

import java.time.Duration;

public class TicketReleaseLockKey implements BasicKey<TicketReleaseLockValue> {
    private static final String PREFIX = "tr:lock:";
    private final String key;

    public TicketReleaseLockKey(String key) {
        this.key = PREFIX + key;
    }

    @Override
    public String get() {
        return key;
    }

    @Override
    public Duration ttl() {
        return Duration.ofMinutes(1);
    }

    @Override
    public Class<TicketReleaseLockValue> type() {
        return TicketReleaseLockValue.class;
    }
}
