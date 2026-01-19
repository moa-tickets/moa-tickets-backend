package stack.moaticket.application.component.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stack.moaticket.application.job.ConcertStartInformJob;

@Component
@RequiredArgsConstructor
public class AlarmScheduler {
    private final ConcertStartInformJob concertStartInformJob;
    private static final String DELAY = "2000";

    @Scheduled(fixedDelayString = DELAY)
    public void concertInform() {
        concertStartInformJob.runEpoch();
    }
}
