package stack.moaticket.system.alarm.core.service;

import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface AlarmSendService {
    void send(Long memberId, AlarmTarget target, AlarmMessage message);
    void sendOrThrow(Long memberId, AlarmTarget target, AlarmMessage message);
    void sendAll(Long memberId, AlarmMessage message);
    void sendToShards(Predicate<EmitterMeta> predicate, Consumer<EmitterMeta> action, int cutoff);
    <T> void sendToShards(Map<Integer, List<T>> shardMap, Consumer<T> action, int cutoff);
}
