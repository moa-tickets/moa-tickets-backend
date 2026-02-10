package stack.moaticket.system.alarm.sse.register;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.util.AlarmShardUtil;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.util.KeyGeneratorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Slf4j
public class SseEmitterRegister {
    private final AtomicLong total = new AtomicLong(0);
    private final ConcurrentMap<Long, ConcurrentHashMap<String, EmitterMeta>> memberEmitterMap = new ConcurrentHashMap<>();

    @Value("${app.server.alarm.shard-count}")
    private int shardCount;

    public String insert(Long memberId, SseEmitter emitter) {
        String connectionId = KeyGeneratorUtil.genUuidV7();
        ConcurrentMap<String, EmitterMeta> inner = memberEmitterMap
                .computeIfAbsent(memberId, k -> new ConcurrentHashMap<>());

        EmitterMeta prev = inner.putIfAbsent(connectionId, new EmitterMeta(memberId, connectionId, emitter));
        if(prev == null) total.incrementAndGet();

        return connectionId;
    }

    public EmitterMeta get(Long memberId, String connectionId) {
        Map<String, EmitterMeta> map = memberEmitterMap.get(memberId);
        return map == null ? null : map.get(connectionId);
    }

    public Map<String, EmitterMeta> getSseEmitters(Long memberId) {
        Map<String, EmitterMeta> metas = memberEmitterMap.get(memberId);
        if(metas == null) return Collections.emptyMap();
        return new HashMap<>(metas);
    }

    public Map<Long, Map<String, EmitterMeta>> getFiltered(Predicate<EmitterMeta> predicate) {
        if(memberEmitterMap.isEmpty()) return Collections.emptyMap();

        Map<Long, Map<String, EmitterMeta>> snapshot = new HashMap<>();
        memberEmitterMap.forEach((mid, inner) -> {
            inner.forEach((cid, meta) -> {
                if(!predicate.test(meta)) return;
                snapshot.computeIfAbsent(mid, k -> new HashMap<>()).put(cid, meta);
            });
        });

        return snapshot;
    }

    public Map<Integer, List<EmitterMeta>> getFilteredForShard(Predicate<EmitterMeta> predicate) {
        if(memberEmitterMap.isEmpty()) return Collections.emptyMap();
        Map<Integer, List<EmitterMeta>> shardMap = AlarmShardUtil.createShardMap(shardCount);

        memberEmitterMap.forEach((mid, inner) -> {
            inner.forEach((cid, meta) -> {
                if(!predicate.test(meta)) return;
                shardMap.get(AlarmShardUtil.getShardNum(mid, shardCount)).add(meta);
            });
        });

        return shardMap;
    }

    public void remove(Long memberId, String connectionId) {
        memberEmitterMap.computeIfPresent(memberId, (id, emitters) -> {
            EmitterMeta removed = emitters.remove(connectionId);
            if(removed != null) {
                total.decrementAndGet();
                removed.markDead();
            }

            return emitters.isEmpty() ? null : emitters;
        });
    }
}
