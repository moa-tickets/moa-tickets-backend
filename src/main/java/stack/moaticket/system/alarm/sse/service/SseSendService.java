package stack.moaticket.system.alarm.sse.service;

import lombok.RequiredArgsConstructor;
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
import java.util.Map;

@RequiredArgsConstructor
public class SseSendService implements AlarmSendService {
    private final SseEmitterRegister sseEmitterRegister;

    @Override
    public void send(Long memberId, AlarmTarget target, AlarmMessage message) {
        String connectionId = target.connectionId();
        EmitterMeta meta = sseEmitterRegister.get(memberId, connectionId);
        sendToMember(memberId, connectionId, meta, message.key(), message.payload(), false);
    }

    @Override
    public void sendOrThrow(Long memberId, AlarmTarget target, AlarmMessage message) {
        String connectionId = target.connectionId();
        EmitterMeta meta = sseEmitterRegister.get(memberId, connectionId);
        sendToMember(memberId, connectionId, meta, message.key(), message.payload(), true);
    }

    @Override
    public void sendAll(Long memberId, AlarmMessage message) {
        Map<String, EmitterMeta> emitterMap = sseEmitterRegister.getSseEmitters(memberId);
        for(Map.Entry<String, EmitterMeta> entry : emitterMap.entrySet()) {
            sendToMember(memberId, entry.getKey(), entry.getValue(), message.key(), message.payload(), false);
        }
    }

    private <T> void sendToMember(Long memberId, String connectionId, EmitterMeta meta, String type, T payload, boolean rethrow) {
        if(meta == null || !meta.isAlive()) return;
        SseEmitter emitter = meta.getEmitter();
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

    private void cleanup(Long memberId, String connectionId, SseEmitter emitter, Exception cause) {
        sseEmitterRegister.remove(memberId, connectionId);
        try { emitter.completeWithError(cause); } catch (Exception ignored) {}
    }
}
