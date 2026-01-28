package stack.moaticket.application.component.scheduler;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import stack.moaticket.application.job.ConcertStartInformJob;
import stack.moaticket.application.job.TicketReleaseInformJob;

import java.time.Duration;

@Component
public class JobScheduler {
    private final TaskScheduler sessionStartScheduler;
    private final TaskScheduler ticketReleaseScheduler;

    private final ConcertStartInformJob concertStartInformJob;
    private final TicketReleaseInformJob ticketReleaseInformJob;

    private final JobSchedulerProperties properties;

    public JobScheduler(
            @Qualifier("sessionStartScheduler") TaskScheduler sessionStartScheduler,
            @Qualifier("ticketReleaseScheduler") TaskScheduler ticketReleaseScheduler,
            ConcertStartInformJob concertStartInformJob,
            TicketReleaseInformJob ticketReleaseInformJob,
            JobSchedulerProperties properties
    ) {
        this.sessionStartScheduler = sessionStartScheduler;
        this.ticketReleaseScheduler = ticketReleaseScheduler;
        this.concertStartInformJob = concertStartInformJob;
        this.ticketReleaseInformJob = ticketReleaseInformJob;
        this.properties = properties;
    }

    private static final int DELAY = 2;

    @PostConstruct
    public void start() {
        if(properties.sessionStart()) {
            sessionStartScheduler.scheduleWithFixedDelay(
                    concertStartInformJob::runEpoch,
                    Duration.ofSeconds(DELAY)
            );
        }

        if(properties.ticketRelease()) {
            ticketReleaseScheduler.scheduleWithFixedDelay(
                    ticketReleaseInformJob::runEpoch,
                    Duration.ofSeconds(DELAY)
            );
        }
    }
}
