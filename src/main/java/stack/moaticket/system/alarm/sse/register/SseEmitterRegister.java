package stack.moaticket.system.alarm.sse.register;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.util.KeyGeneratorUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Slf4j
public class SseEmitterRegister {
    private final ConcurrentMap<Long, ConcurrentHashMap<String, EmitterMeta>> memberEmitterMap = new ConcurrentHashMap<>();

    public String insert(Long memberId, SseEmitter emitter) {
        String connectionId = KeyGeneratorUtil.genUuidV7();
        memberEmitterMap
                .computeIfAbsent(memberId, k -> new ConcurrentHashMap<>())
                .put(connectionId, new EmitterMeta(emitter));
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

    public void remove(Long memberId, String connectionId) {
        memberEmitterMap.computeIfPresent(memberId, (id, emitters) -> {
            emitters.remove(connectionId);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
