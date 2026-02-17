package stack.moaticket.system.redis.component.producer;

import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.redis.model.RedisValue;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

public interface RedisProducer {
    ObjectMapper getMapper();

    default Object[] argsToJson(Object... args) {
        Object[] json = new Object[args.length];

        for(int i=0; i<args.length; i++) {
            Object arg = args[i];

            if(arg == null) {
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }

            if(arg instanceof String s) json[i] = s;
            else if(arg instanceof Number || arg instanceof Boolean) json[i] = String.valueOf(arg);
            else if(arg instanceof Duration d) json[i] = String.valueOf(d.toMillis());
            else if(arg instanceof RedisValue) json[i] = writeJson(arg);
            else json[i] = writeJson(arg);
        }

        return json;
    }

    default String writeJson(Object o) {
        try { return getMapper().writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
