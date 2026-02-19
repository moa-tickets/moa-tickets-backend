package stack.moaticket.system.redis.component.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import stack.moaticket.system.redis.model.RedisKey;

import java.util.List;

@Slf4j
@Component
public class RedisLuaOps implements RedisOps<RedisLuaOps.Bound>{

    @Override
    public Bound bind(StringRedisTemplate template) {
        return new Bound(template);
    }

    @RequiredArgsConstructor
    public static final class Bound {
        private final StringRedisTemplate template;

        <R> R eval(DefaultRedisScript<R> script, List<? extends RedisKey<?>> keys, Object... args) {
            List<String> rawKeys = keys.stream().map(RedisKey::get).toList();
            return template.execute(script, rawKeys, args);
        }
    }
}
