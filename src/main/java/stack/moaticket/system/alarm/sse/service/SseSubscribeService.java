package stack.moaticket.system.alarm.sse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.component.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;

@RequiredArgsConstructor
public class SseSubscribeService {
    private final SseEmitterRegister sseEmitterRegister;
    private final SseSendService sseSendService;

    private static final Long EXPIRE_TIME = 24 * 60 * 60 * 1000L;

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(EXPIRE_TIME);
        String connectionId = sseEmitterRegister.insert(memberId, emitter);

        AlarmTarget target = new AlarmTarget(connectionId);
        AlarmMessage message = AlarmMessageFactory.connect();

        try {
            sseSendService.sendOrThrow(memberId, target, message);
            afterClean(memberId, emitter, connectionId);
            return emitter;
        } catch (RuntimeException e) {
            sseEmitterRegister.remove(memberId, connectionId);
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
            throw e;
        }
    }

    private void afterClean(Long memberId, SseEmitter emitter, String connectionId) {
        Runnable cleanup = () -> sseEmitterRegister.remove(memberId, connectionId);

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
    }
 }
