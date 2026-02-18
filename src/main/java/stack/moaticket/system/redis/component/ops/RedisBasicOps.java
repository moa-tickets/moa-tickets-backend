package stack.moaticket.system.redis.component.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.redis.model.BasicKey;
import stack.moaticket.system.redis.model.RedisValue;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBasicOps implements RedisOps<RedisBasicOps.Bound> {
    private final ObjectMapper mapper;

    @Override
    public Bound bind(StringRedisTemplate template) {
        return new Bound(template, mapper);
    }

    @RequiredArgsConstructor
    public static final class Bound {
        private final StringRedisTemplate template;
        private final ObjectMapper mapper;

        <T extends RedisValue> void set(BasicKey<T> key, T value) {
            try {
                template.opsForValue().set(
                        key.get(),
                        mapper.writeValueAsString(value),
                        key.ttl());
            } catch (Exception e) {
                log.error("RedisBasicOps: Redis serialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> boolean setIfAbsent(BasicKey<T> key, T value) {
            try {
                Boolean ok = template.opsForValue().setIfAbsent(
                        key.get(),
                        mapper.writeValueAsString(value),
                        key.ttl());
                return Boolean.TRUE.equals(ok);
            } catch (Exception e) {
                log.error("RedisBasicOps: Redis serialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> T get(BasicKey<T> key) {
            String json = template.opsForValue().get(key.get());
            if(json == null) return null;

            try {
                return mapper.readValue(json, key.type());
            } catch (Exception e) {
                log.error("RedisBasicOps: Redis deserialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> boolean isExist(BasicKey<T> key) {
            try {
                Boolean ok = template.hasKey(key.get());
                return Boolean.TRUE.equals(ok);
            } catch (Exception e) {
                log.error("RedisBasicOps: Redis serialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> boolean remove(BasicKey<T> key) {
            Boolean deleted = template.delete(key.get());
            return Boolean.TRUE.equals(deleted);
        }
    }
}
