package stack.moaticket.system.alarm.sse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import stack.moaticket.system.alarm.core.component.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;
import stack.moaticket.system.exception.MoaException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SseHeartbeatService {
    private final SseEmitterRegister sseEmitterRegister;
    private final AsyncSseSendService asyncSseSendService;

    public void sendHeartbeat() {
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Map<String, EmitterMeta>> receiver = sseEmitterRegister
                .getFiltered(meta -> meta.tryMarkHeartbeat(now));

        AlarmMessage message = AlarmMessageFactory.heartbeat();

        receiver.forEach((mid, inner) -> {
            inner.forEach((cid, meta) -> {
                AlarmTarget target = new AlarmTarget(cid);
                try {
                    asyncSseSendService.sendOrThrow(mid, target, message);
                } catch (MoaException e) {
                    log.info("Heartbeat send is failed - id: {}, connId: {}", mid, cid);
                }
            });
        });
    }
}
