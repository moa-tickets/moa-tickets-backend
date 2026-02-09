package stack.moaticket.system.alarm.sse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import stack.moaticket.system.alarm.core.util.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

        Map<Integer, List<EmitterMeta>> receiver = sseEmitterRegister
                .getFilteredForShard(meta -> meta.tryMarkHeartbeat(now));

        AlarmMessage message = AlarmMessageFactory.heartbeat();

        int cutoff = 200;

        receiver.forEach((shardKey, metaList) -> {
            int size = metaList.size();

            for(int start = 0; start < size; start += cutoff) {
                int end = Math.min(start + cutoff, size);

                final int s = start;
                final int e = end;

                asyncExecutor.execute(() -> {
                    for(int i = s; i< e; i++) {
                        EmitterMeta meta = metaList.get(i);

                        Long memberId = meta.getMemberId();
                        AlarmTarget target = new AlarmTarget(meta.getConnectionId());
                        sseSendService.send(memberId, target, message);
                    }
                });
            }
        });
    }
}
