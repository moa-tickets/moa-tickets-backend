package stack.moaticket.application.component.register;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class AlarmEmitterRegister {
    private final ConcurrentMap<Long, Set<SseEmitter>> memberEmitterMap = new ConcurrentHashMap<>();

    public void insert(Long memberId, SseEmitter emitter) {
        memberEmitterMap
                .computeIfAbsent(memberId, k -> ConcurrentHashMap.newKeySet())
                .add(emitter);
    }

    public List<SseEmitter> getSseEmitters(Long memberId) {
        Set<SseEmitter> emitters = memberEmitterMap.get(memberId);
        if(emitters == null) return null;
        return new ArrayList<>(emitters);
    }

    public void remove(Long memberId, SseEmitter emitter) {
        memberEmitterMap.computeIfPresent(memberId, (id, emitters) -> {
            emitters.remove(emitter);
            if(emitters.isEmpty()) {
                return null;
            }
            return emitters;
        });

        log.info("AlarmEmitterRegister: {}-{} is removed", memberId, emitter.toString());
    }
}
