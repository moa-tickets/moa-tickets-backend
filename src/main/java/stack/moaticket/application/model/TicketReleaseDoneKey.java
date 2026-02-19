package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.BasicKey;

import java.time.Duration;

public class TicketReleaseDoneKey implements BasicKey<TicketReleaseDoneValue> {
    private static final String PREFIX = "tr:done:";
    private final String key;

    public TicketReleaseDoneKey(String key) {
        this.key = PREFIX + key;
    }

    @Override
    public String get() {
        return key;
    }

    @Override
    public Duration ttl() {
        return Duration.ofMinutes(10);
    }

    @Override
    public Class<TicketReleaseDoneValue> type() {
        return TicketReleaseDoneValue.class;
    }
}
