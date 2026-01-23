package stack.moaticket.system.sse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.sse.register.SseEmitterRegister;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SseSendService {
    private final SseEmitterRegister sseEmitterRegister;

    public <T> void send(Long memberId, SseEmitter emitter, String type, T payload) {
        sendToMember(memberId, emitter, type, payload, false);
    }

    public <T> void sendOrThrow(Long memberId, SseEmitter emitter, String type, T payload) {
        sendToMember(memberId, emitter, type, payload, true);
    }

    public <T> void sendAll(Long memberId, String type, T payload) {
        List<SseEmitter> emitterList = sseEmitterRegister.getSseEmitters(memberId);
        if(emitterList == null) return;
        for(SseEmitter emitter : emitterList) {
            sendToMember(memberId, emitter, type, payload, false);
        }
    }

    private <T> void sendToMember(Long memberId, SseEmitter emitter, String type, T payload, boolean rethrow) {
        try {
            emitter.send(SseEmitter
                    .event()
                    .name(type)
                    .data(payload));
        } catch (IOException | IllegalStateException e) {
            cleanup(memberId, emitter, e);
            if(rethrow) {
                throw new MoaException(MoaExceptionType.SSE_ERROR);
            }
        }
    }

    private void cleanup(Long memberId, SseEmitter emitter, Exception cause) {
        sseEmitterRegister.remove(memberId, emitter);
        try { emitter.completeWithError(cause); } catch (Exception ignored) {}
    }
}
