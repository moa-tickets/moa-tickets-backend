package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.RedisValue;

public record TicketReleaseLockValue(
        boolean lock
) implements RedisValue {}
