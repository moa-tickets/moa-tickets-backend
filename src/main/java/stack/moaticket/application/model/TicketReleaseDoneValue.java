package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.RedisValue;

public record TicketReleaseDoneValue(
        boolean done
) implements RedisValue {}