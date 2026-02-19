package stack.moaticket.system.redis.component.ops;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import stack.moaticket.system.redis.model.*;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class RedisClient {
    private final RedisOpsRouter router;
    private final StringRedisTemplate inner;
    private final StringRedisTemplate outer;

    public RedisClient(
            RedisOpsRouter router,
            @Qualifier("innerStringRedisTemplate") StringRedisTemplate inner,
            @Qualifier("outerStringRedisTemplate") StringRedisTemplate outer) {
        this.inner = inner;
        this.outer = outer;
        this.router = router;
    }

    public interface RedisBound {
        Basic basic();
        Zset zset();
        Stream stream();
        Lua lua();
    }

    public RedisBound inner() { return new Bound(router, inner); }
    public RedisBound outer() { return new Bound(router, outer); }

    private record Bound(
            RedisOpsRouter router,
            StringRedisTemplate template) implements RedisBound {
        @Override
        public Basic basic() {
            return new Basic(router.find(RedisBasicOps.class).bind(template));
        }
        @Override
        public Zset zset() {
            return new Zset(router.find(RedisZsetOps.class).bind(template));
        }
        @Override
        public Stream stream() {
            return new Stream(router.find(RedisStreamOps.class).bind(template));
        }
        @Override
        public Lua lua() {
            return new Lua(router.find(RedisLuaOps.class).bind(template));
        }
    }

    public record Basic(RedisBasicOps.Bound ops) {
        public <T extends RedisValue> void set(BasicKey<T> key, T value) {
            ops.set(key, value);
        }

        public <T extends RedisValue> boolean setIfAbsent(BasicKey<T> key, T value) {
            return ops.setIfAbsent(key, value);
        }

        public <T extends RedisValue> T get(BasicKey<T> key) {
            return ops.get(key);
        }

        public <T extends RedisValue> boolean isExist(BasicKey<T> key) {
            return ops.isExist(key);
        }

        public <T extends RedisValue> boolean remove(BasicKey<T> key) {
            return ops.remove(key);
        }
    }

    public record Zset(RedisZsetOps.Bound ops) {
        public <T extends RedisValue> void add(ZsetKey<T> key, T value, double score) {
            ops.add(key, value, score);
        }

        public <T extends RedisValue> boolean addIfAbsent(ZsetKey<T> key, T value, double score) {
            return ops.addIfAbsent(key, value, score);
        }

        public <T extends RedisValue> T popMin(ZsetKey<T> key) {
            return ops.popMin(key);
        }

        public <T extends RedisValue> List<T> popMin(ZsetKey<T> key, long count) {
            return ops.popMin(key, count);
        }

        public <T extends RedisValue> T popMax(ZsetKey<T> key) {
            return ops.popMax(key);
        }

        public <T extends RedisValue> List<T> popMax(ZsetKey<T> key, long count) {
            return ops.popMax(key, count);
        }
    }

    public record Stream(RedisStreamOps.Bound ops) {
        public <T extends RedisValue> RecordId xAdd(StreamKey<T> key, T value, long expiresAt) {
            return ops.xAdd(key, value, expiresAt);
        }

        public <T extends RedisValue> void createGroupIfAbsent(StreamKey<T> key, String group) {
            ops.createGroupIfAbsent(key, group);
        }

        public <T extends RedisValue> StreamMessage<T> xRead(
                StreamKey<T> key,
                String group,
                String consumerName,
                Duration block) {
            return ops.xRead(key, group, consumerName, block);
        }

        public <T extends RedisValue> StreamMessage<T> xAutoClaim(
                StreamKey<T> key,
                String group,
                String consumerName) {
            return ops.xAutoClaim(key, group, consumerName);
        }

        public <T extends RedisValue> long xAck(StreamKey<T> key, String group, RecordId... ids) {
            return ops.xAck(key, group, ids);
        }
    }

    public record Lua(RedisLuaOps.Bound ops) {
        public <R> R eval(DefaultRedisScript<R> script, List<? extends RedisKey<?>> keys, Object... args) {
            return ops.eval(script, keys, args);
        }
    }
}