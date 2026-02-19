package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.BasicKey;
import stack.moaticket.system.util.KeyGeneratorUtil;

import java.time.Duration;

public class TicketReleaseRunKey implements BasicKey<TicketReleaseRunValue> {
    private static final String PREFIX = "tr:runId:";
    private final String key;

    private TicketReleaseRunKey() {
        this.key = PREFIX + KeyGeneratorUtil.genUuidV4();
    }

    private TicketReleaseRunKey(String entireKey) {
        this.key = entireKey;
    }

    public static TicketReleaseRunKey create() {
        return new TicketReleaseRunKey();
    }

    public static TicketReleaseRunKey create(String key, boolean prefix) {
        if(prefix) return new TicketReleaseRunKey(PREFIX + key);
        else return new TicketReleaseRunKey(key);
    }

    @Override
    public String get() {
        return key;
    }

    @Override
    public Class<TicketReleaseRunValue> type() {
        return TicketReleaseRunValue.class;
    }

    @Override
    public Duration ttl() {
        return Duration.ofMinutes(5);
    }
}
