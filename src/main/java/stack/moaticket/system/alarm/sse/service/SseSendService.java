package stack.moaticket.system.alarm.sse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.core.service.AlarmSendService;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class SseSendService implements AlarmSendService {
    private final SseEmitterRegister sseEmitterRegister;
    private final Executor asyncExecutor;

    public SseSendService(
            SseEmitterRegister sseEmitterRegister,
            @Qualifier("asyncExecutor") Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        this.sseEmitterRegister = sseEmitterRegister;
    }

    @Override
    public void send(Long memberId, AlarmTarget target, AlarmMessage message) {
        String connectionId = target.connectionId();
        EmitterMeta meta = sseEmitterRegister.get(memberId, connectionId);
        sendToMember(meta, message, false);
    }

    @Override
    public void sendOrThrow(Long memberId, AlarmTarget target, AlarmMessage message) {
        String connectionId = target.connectionId();
        EmitterMeta meta = sseEmitterRegister.get(memberId, connectionId);
        sendToMember(meta, message, true);
    }

    @Override
    public void sendAll(Long memberId, AlarmMessage message) {
        Map<String, EmitterMeta> emitterMap = sseEmitterRegister.getSseEmitters(memberId);
        for(Map.Entry<String, EmitterMeta> entry : emitterMap.entrySet()) {
            sendToMember(entry.getValue(), message, false);
        }
    }

    @Override
    public void sendToShards(
            Predicate<EmitterMeta> predicate,
            Consumer<EmitterMeta> action,
            int cutoff) {
        Map<Integer, List<EmitterMeta>> receiver = sseEmitterRegister.getFilteredForShard(predicate);

        receiver.forEach((shardKey, metaList) -> {
            internalShard(metaList, action, cutoff);
        });
    }

    @Override
    public <T> void sendToShards(
            Map<Integer, List<T>> shardMap,
            Consumer<T> action,
            int cutoff) {
        shardMap.forEach((shardKey, metaList) ->
            internalShard(metaList, action, cutoff)
        );
    }

    private void sendToMember(EmitterMeta meta, AlarmMessage message, boolean rethrow) {
        if(meta == null || !meta.isAlive()) return;

        Long memberId = meta.getMemberId();
        String connectionId = meta.getConnectionId();
        SseEmitter emitter = meta.getEmitter();
        String type = message.key();
        Object payload = message.payload();

        try {
            emitter.send(SseEmitter
                    .event()
                    .name(type)
                    .data(payload));
            meta.updateLastSentAt(LocalDateTime.now());
        } catch (IOException | IllegalStateException e) {
            if(meta.markDead()) {
                cleanup(memberId, connectionId, emitter, e);
            }
            if(rethrow) {
                throw new MoaException(MoaExceptionType.SSE_ERROR);
            }
        }
    }

    private <T> void internalShard(List<T> metaList, Consumer<T> action, int cutoff) {
        int size = metaList.size();

        for(int start = 0; start < size; start += cutoff) {
            int end = Math.min(start + cutoff, size);

            final int s = start;
            final int e = end;

            asyncExecutor.execute(() -> {
                try {
                    for(int i = s; i < e; i++) {
                        action.accept(metaList.get(i));
                    }
                } catch (Exception ex) {
                    log.warn("Shard task failed. range=[{}, {}]", s, e, ex);
                }

            });
        }
    }

    private void cleanup(Long memberId, String connectionId, SseEmitter emitter, Exception cause) {
        sseEmitterRegister.remove(memberId, connectionId);
        try { emitter.completeWithError(cause); } catch (Exception ignored) {}
    }
}
