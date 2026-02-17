package stack.moaticket.system.redis.model;

import java.time.Duration;

public interface BasicKey<T extends RedisValue> extends RedisKey<T> {
    Duration ttl();
}
