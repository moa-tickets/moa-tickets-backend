package stack.moaticket.system.alarm.sse.component.scheduler;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import stack.moaticket.system.alarm.sse.job.SseHeartbeatInformJob;

import java.time.Duration;

public class SseHeartbeatScheduler {
    private final TaskScheduler heartbeatScheduler;
    private final SseHeartbeatInformJob sseHeartbeatInformJob;

    public SseHeartbeatScheduler(
            @Qualifier("heartbeatInformScheduler") TaskScheduler heartbeatScheduler,
            SseHeartbeatInformJob sseHeartbeatInformJob
    ) {
        this.heartbeatScheduler = heartbeatScheduler;
        this.sseHeartbeatInformJob = sseHeartbeatInformJob;
    }

    private static final int RATE = 25;

    @PostConstruct
    public void start() {
        heartbeatScheduler.scheduleAtFixedRate(
                sseHeartbeatInformJob::run,
                Duration.ofSeconds(RATE)
        );
    }
}
