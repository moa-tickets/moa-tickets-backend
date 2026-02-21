package stack.moaticket.system.redis.component.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.redis.model.StreamKey;
import stack.moaticket.system.redis.model.RedisValue;
import stack.moaticket.system.redis.model.StreamMessage;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamOps implements RedisOps<RedisStreamOps.Bound> {
    private final ObjectMapper mapper;

    @Override
    public Bound bind(StringRedisTemplate template) {
        return new Bound(template, mapper);
    }

    @RequiredArgsConstructor
    public static final class Bound {
        private final StringRedisTemplate template;
        private final ObjectMapper mapper;

        private static final String PAYLOAD = "payload";
        private static final String EXPIRES_AT = "expiresAt";

        <T extends RedisValue> RecordId xAdd(StreamKey<T> key, T value, long expiresAt) {
            try {
                String json = mapper.writeValueAsString(value);
                MapRecord<String, String, String> record = StreamRecords
                        .newRecord()
                        .in(key.get())
                        .ofMap(Map.of(
                                PAYLOAD, json,
                                EXPIRES_AT, String.valueOf(expiresAt)));
                RecordId id = template.opsForStream().add(record);
                if(id == null) {
                    log.error("RedisStreamOps: XADD returned null (key={})", key.get());
                    throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
                }
                return id;
            } catch (Exception e) {
                log.error("RedisStreamOps: Redis serialize failed", e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> void createGroupIfAbsent(StreamKey<T> key, String group) {
            try {
                template.opsForStream().createGroup(key.get(), ReadOffset.latest(), group);
            } catch (RedisSystemException e) {
                if(!(e.getCause() instanceof io.lettuce.core.RedisBusyException)) {
                    throw e;
                }
            } catch (Exception e) {
                log.error("RedisStreamOps: create group is failed (key={}, group={})", key.get(), group, e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        <T extends RedisValue> StreamMessage<T> xRead(
                StreamKey<T> key,
                String group,
                String consumerName,
                Duration block) {
            var options = StreamReadOptions.empty().count(1);
            if(block != null && !block.isZero() && !block.isNegative()) {
                options = options.block(block);
            }

            List<MapRecord<String, Object, Object>> records =
                    template.opsForStream().read(
                            Consumer.from(group, consumerName),
                            options,
                            StreamOffset.create(key.get(), ReadOffset.lastConsumed()));

            if(records == null || records.isEmpty()) return null;
            MapRecord<String, Object, Object> record = records.getFirst();

            try {
                Object payloadObj = record.getValue().get(PAYLOAD);
                if(payloadObj == null) return null;

                String payloadJson = String.valueOf(payloadObj);

                Object expiresAtObj = record.getValue().get(EXPIRES_AT);
                if(expiresAtObj == null) return null;

                T payload = mapper.readValue(payloadJson, key.type());
                long expiresAt = Long.parseLong(String.valueOf(expiresAtObj));

                return new StreamMessage<>(record.getId(), payload, expiresAt);
            } catch (Exception e) {
                log.error("RedisStreamOps: Redis deserialize is failed (key={}, group={}, consumer={})", key.get(), group, consumerName, e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        public <T extends RedisValue> StreamMessage<T> xAutoClaim(
                StreamKey<T> key,
                String group,
                String consumerName,
                Duration lockMillis) {
            PendingMessages pending = template.opsForStream().pending(
                    key.get(),
                    group,
                    Range.unbounded(),
                    1L,
                    lockMillis);
            if(pending == null || pending.isEmpty()) return null;

            RecordId id = pending.get(0).getId();

            List<MapRecord<String, Object, Object>> claimed = template.opsForStream().claim(
                    key.get(),
                    group,
                    consumerName,
                    lockMillis,
                    id);
            if(claimed == null || claimed.isEmpty()) return null;
            MapRecord<String, Object, Object> record = claimed.getFirst();

            try {
                Object payloadObj = record.getValue().get(PAYLOAD);
                if(payloadObj == null) return null;

                String payloadJson = String.valueOf(payloadObj);

                Object expiresAtObj = record.getValue().get(EXPIRES_AT);
                if(expiresAtObj == null) return null;

                T payload = mapper.readValue(payloadJson, key.type());
                long expiresAt = Long.parseLong(String.valueOf(expiresAtObj));

                return new StreamMessage<>(record.getId(), payload, expiresAt);
            } catch (Exception e) {
                log.error("RedisStreamOps: Redis deserialize is failed (key={}, group={}, consumer={})", key.get(), group, consumerName, e);
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }

        public <T extends RedisValue> long xAck(StreamKey<T> key, String group, RecordId... ids) {
            if(ids == null || ids.length == 0) return 0L;
            Long acked = template.opsForStream().acknowledge(key.get(), group, ids);
            return acked == null ? 0L : acked;
        }
    }
}
