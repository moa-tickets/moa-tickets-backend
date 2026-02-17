package stack.moaticket.system.redis.model;

import org.springframework.data.redis.connection.stream.RecordId;

public record StreamMessage<T extends RedisValue>(
        RecordId id,
        T payload,
        long expiresAt
) {}
