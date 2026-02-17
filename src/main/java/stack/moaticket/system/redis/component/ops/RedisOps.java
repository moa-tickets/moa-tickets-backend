package stack.moaticket.system.redis.component.ops;

import org.springframework.data.redis.core.StringRedisTemplate;

public interface RedisOps<B> {
    B bind(StringRedisTemplate template);
}
