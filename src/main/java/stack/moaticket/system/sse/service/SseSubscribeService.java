package stack.moaticket.system.sse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.sse.register.SseEmitterRegister;

@Service
@RequiredArgsConstructor
public class SseSubscribeService {
    private final SseEmitterRegister sseEmitterRegister;
    private final SseSendService sseSendService;

    private static final String CONNECT_EVENT_TYPE = "connect";
    private static final Long EXPIRE_TIME = 24 * 60 * 60 * 1000L;

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(EXPIRE_TIME);
        sseEmitterRegister.insert(memberId, emitter);

        try {
            sseSendService.sendOrThrow(memberId, emitter, CONNECT_EVENT_TYPE, null);
            afterClean(memberId, emitter);
            return emitter;
        } catch (RuntimeException e) {
            sseEmitterRegister.remove(memberId, emitter);
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
            throw e;
        }
    }

    private void afterClean(Long memberId, SseEmitter emitter) {
        Runnable cleanup = () -> sseEmitterRegister.remove(memberId, emitter);

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
    }
 }
