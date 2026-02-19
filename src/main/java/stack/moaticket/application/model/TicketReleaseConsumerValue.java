package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.RedisValue;
import stack.moaticket.system.util.KeyGeneratorUtil;

import java.time.Duration;

public record TicketReleaseConsumerValue(
        String id,
        String refKey,
        Long cursor
) implements RedisValue {
    public static String createId() {
        return KeyGeneratorUtil.genUuidV4();
    }

    public static Long createExpiresAtMillis() {
        return System.currentTimeMillis() + Duration.ofMinutes(5).toMillis();
    }
}
