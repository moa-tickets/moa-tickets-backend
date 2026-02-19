package stack.moaticket.system.redis.model;

public interface RedisKey<T extends RedisValue> {
    String get();
    Class<T> type();
}
