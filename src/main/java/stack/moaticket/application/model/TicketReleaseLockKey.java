package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.BasicKey;

import java.time.Duration;

public class TicketReleaseLockKey implements BasicKey<TicketReleaseLockValue> {
    private static final String PREFIX = "tr:lock:";
    private final String key;
    private final Duration ttl;

    public TicketReleaseLockKey(String key, Duration ttl) {
        this.key = PREFIX + key;
        this.ttl = ttl;
    }

    @Override
    public String get() {
        return key;
    }

    @Override
    public Duration ttl() {
        return ttl;
    }

    @Override
    public Class<TicketReleaseLockValue> type() {
        return TicketReleaseLockValue.class;
    }
}
