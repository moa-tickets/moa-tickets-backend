package stack.moaticket.system.redis.component.ops;

import lombok.RequiredArgsConstructor;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.Map;

@RequiredArgsConstructor
public class RedisOpsRouter {
    private final Map<Class<? extends RedisOps<?>>, RedisOps<?>> routeMap;

    @SuppressWarnings("unchecked")
    public <B, O extends RedisOps<B>> O find(Class<O> type) {
        RedisOps<?> ops = routeMap.get(type);
        if(ops == null) {
            throw new MoaException(MoaExceptionType.MISMATCH_ARGUMENT);
        }
        return (O) ops;
    }
}