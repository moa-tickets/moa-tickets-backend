package stack.moaticket.application.component.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import stack.moaticket.application.component.gauge.TicketReleaseRedisGaugeManager;
import stack.moaticket.application.model.TicketReleaseConsumerKey;
import stack.moaticket.application.model.TicketReleaseConsumerValue;
import stack.moaticket.system.redis.component.ops.RedisClient;
import stack.moaticket.system.redis.component.producer.RedisProducer;
import stack.moaticket.system.redis.model.RedisKey;
import stack.moaticket.system.redis.model.RedisValue;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketReleaseProducer implements RedisProducer {
    private final RedisClient redis;
    private final ObjectMapper mapper;
    private final TicketReleaseRedisGaugeManager manager;
    private final TicketReleaseConsumerKey consumerKey = new TicketReleaseConsumerKey();

    @Override
    public ObjectMapper getMapper() {
        return mapper;
    }

    public void publishFirst(List<RedisKey<? extends RedisValue>> keys, Object... args) {
        manager.recordProducer(() -> {
            Object[] json = argsToJson(args);
            redis.inner().lua().eval(scriptFirst(), keys, json);
        });
    }

    public void publishContinue(TicketReleaseConsumerValue payload, Long expiresAtMillis) {
        manager.recordProducer(() -> redis.inner().stream().xAdd(
                consumerKey,
                payload,
                expiresAtMillis
        ));
    }

    private DefaultRedisScript<Void> scriptFirst() {
        DefaultRedisScript<Void> script = new DefaultRedisScript<>();
        script.setResultType(Void.class);
        script.setScriptText("""
                redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
                local id = redis.call('XADD', KEYS[2], '*',
                    'refKey', KEYS[1],
                    'payload', ARGV[3],
                    'expiresAt', ARGV[4]);
                """);
        return script;
    }
}
