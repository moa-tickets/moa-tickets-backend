package stack.moaticket.system.alarm.sse.service;

import lombok.extern.slf4j.Slf4j;
import stack.moaticket.system.alarm.core.model.HeartbeatPayload;
import stack.moaticket.system.alarm.core.util.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;

@Slf4j
public class SseHeartbeatService {
    private final SseSendService sseSendService;

    public SseHeartbeatService(
            SseSendService sseSendService) {
        this.sseSendService = sseSendService;
    }

    public void sendHeartbeat() {
        long now = System.currentTimeMillis();
        AlarmMessage<HeartbeatPayload> message = AlarmMessageFactory.heartbeat();
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
