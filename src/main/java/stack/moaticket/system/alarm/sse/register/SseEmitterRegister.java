package stack.moaticket.system.alarm.sse.register;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.util.KeyGeneratorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class SseEmitterRegister {
    private final ConcurrentMap<Long, ConcurrentHashMap<String, SseEmitter>> memberEmitterMap = new ConcurrentHashMap<>();

    public String insert(Long memberId, SseEmitter emitter) {
        String connectionId = KeyGeneratorUtil.genUuidV7();
        memberEmitterMap
                .computeIfAbsent(memberId, k -> new ConcurrentHashMap<>())
                .put(connectionId, emitter);
        return connectionId;
    }

    public SseEmitter get(Long memberId, String connectionId) {
        Map<String, SseEmitter> map = memberEmitterMap.get(memberId);
        return map == null ? null : map.get(connectionId);
    }

    public Map<String, SseEmitter> getSseEmitters(Long memberId) {
        Map<String, SseEmitter> emitters = memberEmitterMap.get(memberId);
        if(emitters == null) return Collections.emptyMap();
        return new HashMap<>(emitters);
    }

    public void remove(Long memberId, String connectionId) {
        memberEmitterMap.computeIfPresent(memberId, (id, emitters) -> {
            emitters.remove(connectionId);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
