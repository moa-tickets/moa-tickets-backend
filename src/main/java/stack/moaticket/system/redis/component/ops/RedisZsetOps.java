package stack.moaticket.system.redis.component.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.redis.model.RedisValue;
import stack.moaticket.system.redis.model.ZsetKey;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisZsetOps implements RedisOps<RedisZsetOps.Bound> {
    private final ObjectMapper mapper;

    @Override
    public Bound bind(StringRedisTemplate template) {
        return new Bound(template, mapper);
    }

    @RequiredArgsConstructor
    public static final class Bound {
        private final StringRedisTemplate template;
        private final ObjectMapper mapper;

        <T extends RedisValue> void add(ZsetKey<T> key, T value, double score) {
            try {
                template.opsForZSet().add(
                        key.get(),
                        mapper.writeValueAsString(value),
                        score);
            } catch (Exception e) {
                log.error("RedisZsetOps: Redis serialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> boolean addIfAbsent(ZsetKey<T> key, T value, double score) {
            try {
                Boolean ok = template.opsForZSet().addIfAbsent(
                        key.get(),
                        mapper.writeValueAsString(value),
                        score);
                return Boolean.TRUE.equals(ok);
            } catch (Exception e) {
                log.error("RedisZsetOps: Redis serialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> T popMin(ZsetKey<T> key) {
            ZSetOperations.TypedTuple<String> tuple = template.opsForZSet().popMin(key.get());
            if(tuple == null || tuple.getValue() == null) return null;

            try {
                return mapper.readValue(tuple.getValue(), key.type());
            } catch (Exception e) {
                log.error("RedisZsetOps: Redis deserialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> List<T> popMin(ZsetKey<T> key, long count) {
            Set<ZSetOperations.TypedTuple<String>> tuples = template.opsForZSet().popMin(key.get(), count);
            if(tuples == null || tuples.isEmpty()) return List.of();

            List<T> result = new ArrayList<>(tuples.size());
            for(var t : tuples) {
                String json = t.getValue();
                if(json == null) continue;

                try {
                    result.add(mapper.readValue(json, key.type()));
                } catch (Exception e) {
                    log.error("RedisZsetOps: Redis deserialize failed", e);
                    throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
                }
            }
            return result;
        }

        <T extends RedisValue> T popMax(ZsetKey<T> key) {
            ZSetOperations.TypedTuple<String> tuple = template.opsForZSet().popMax(key.get());
            if(tuple == null || tuple.getValue() == null) return null;

            try {
                return mapper.readValue(tuple.getValue(), key.type());
            } catch (Exception e) {
                log.error("RedisZsetOps: Redis deserialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> List<T> popMax(ZsetKey<T> key, long count) {
            Set<ZSetOperations.TypedTuple<String>> tuples = template.opsForZSet().popMax(key.get(), count);
            if(tuples == null || tuples.isEmpty()) return List.of();

            List<T> result = new ArrayList<>(tuples.size());
            for(var t : tuples) {
                String json = t.getValue();
                if(json == null) continue;

                try {
                    result.add(mapper.readValue(json, key.type()));
                } catch (Exception e) {
                    log.error("RedisZsetOps: Redis deserialize failed", e);
                    throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
                }
            }
            return result;
        }
    }
}
