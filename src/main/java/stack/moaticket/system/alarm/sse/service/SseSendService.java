package stack.moaticket.system.alarm.sse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.core.service.AlarmSendService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;

import java.io.IOException;
import java.util.Map;

@ConditionalOnProperty(
        value = "alarm.sender",
        havingValue = "sse",
        matchIfMissing = true
)
@Service
@RequiredArgsConstructor
public class SseSendService implements AlarmSendService {
    private final SseEmitterRegister sseEmitterRegister;

    @Override
    public void send(Long memberId, AlarmTarget target, AlarmMessage message) {
        sendToMember(memberId, target.connectionId(), message.key(), message.payload(), false);
    }

    @Override
    public void sendOrThrow(Long memberId, AlarmTarget target, AlarmMessage message) {
        sendToMember(memberId, target.connectionId(), message.key(), message.payload(), true);
    }

    @Override
    public void sendAll(Long memberId, AlarmMessage message) {
        Map<String, SseEmitter> emitterMap = sseEmitterRegister.getSseEmitters(memberId);
        for(Map.Entry<String, SseEmitter> emitter : emitterMap.entrySet()) {
            sendToMember(memberId, emitter.getKey(), message.key(), message.payload(), false);
        }
    }

    private <T> void sendToMember(Long memberId, String connectionId, String type, T payload, boolean rethrow) {
        SseEmitter emitter = sseEmitterRegister.get(memberId, connectionId);
        try {
            if(emitter == null) return;

            emitter.send(SseEmitter
                    .event()
                    .name(type)
                    .data(payload));
        } catch (IOException | IllegalStateException e) {
            cleanup(memberId, connectionId, emitter, e);
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
