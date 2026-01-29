package stack.moaticket.system.alarm.sse.job;

import lombok.RequiredArgsConstructor;
import stack.moaticket.system.alarm.sse.service.SseHeartbeatService;

@RequiredArgsConstructor
public class SseHeartbeatInformJob {
    private final SseHeartbeatService sseHeartbeatService;

    public void run() {
        sseHeartbeatService.sendHeartbeat();
    }
}
