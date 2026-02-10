package stack.moaticket.system.alarm.sse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import stack.moaticket.system.alarm.core.util.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

@Slf4j
public class SseHeartbeatService {
    private final SseEmitterRegister sseEmitterRegister;
    private final SseSendService sseSendService;
    private final Executor asyncExecutor;

    public SseHeartbeatService(
            SseEmitterRegister sseEmitterRegister,
            SseSendService sseSendService,
            @Qualifier("asyncExecutor") Executor asyncExecutor) {
        this.sseEmitterRegister = sseEmitterRegister;
        this.sseSendService = sseSendService;
        this.asyncExecutor = asyncExecutor;
    }

    public void sendHeartbeat() {
        LocalDateTime now = LocalDateTime.now();
        AlarmMessage message = AlarmMessageFactory.heartbeat();
        final int cutoff = 200;

        sseSendService.sendToShards(
                meta -> meta.tryMarkHeartbeat(now),
                meta -> {
                    Long memberId = meta.getMemberId();
                    AlarmTarget target = new AlarmTarget(meta.getConnectionId());
                    sseSendService.send(memberId, target, message);
                },
                cutoff
        );
    }
}
