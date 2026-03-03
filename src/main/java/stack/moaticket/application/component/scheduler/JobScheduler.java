package stack.moaticket.application.component.scheduler;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import stack.moaticket.application.job.ChatMessageBulkJob;
import stack.moaticket.application.job.ConcertStartInformJob;
import stack.moaticket.application.job.TicketReleaseInformJob;

import java.time.Duration;

@Component
public class JobScheduler {
    private final TaskScheduler sessionStartScheduler;
    private final TaskScheduler ticketReleaseScheduler;
    private final TaskScheduler chatMessageBulkScheduler;

    private final ConcertStartInformJob concertStartInformJob;
    private final TicketReleaseInformJob ticketReleaseInformJob;
    private final ChatMessageBulkJob chatMessageBulkJob;

    private final JobSchedulerProperties properties;

    public JobScheduler(
            @Qualifier("sessionStartScheduler") TaskScheduler sessionStartScheduler,
            @Qualifier("ticketReleaseScheduler") TaskScheduler ticketReleaseScheduler,
            @Qualifier("chatMessageBulkScheduler") TaskScheduler chatMessageBulkScheduler,
            ConcertStartInformJob concertStartInformJob,
            TicketReleaseInformJob ticketReleaseInformJob,
            ChatMessageBulkJob chatMessageBulkJob,
            JobSchedulerProperties properties
    ) {
        this.sessionStartScheduler = sessionStartScheduler;
        this.ticketReleaseScheduler = ticketReleaseScheduler;
        this.chatMessageBulkScheduler = chatMessageBulkScheduler;
        this.concertStartInformJob = concertStartInformJob;
        this.ticketReleaseInformJob = ticketReleaseInformJob;
        this.chatMessageBulkJob = chatMessageBulkJob;
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        if(properties.sessionStart().enabled()) {
            sessionStartScheduler.scheduleAtFixedRate(
                    concertStartInformJob::runEpoch,
                    Duration.ofMillis(properties.sessionStart().rateMillis())
            );
        }

        if(properties.ticketRelease().enabled()) {
            ticketReleaseScheduler.scheduleAtFixedRate(
                    ticketReleaseInformJob::runEpoch,
                    Duration.ofMillis(properties.ticketRelease().rateMillis())
            );
        }

        if(properties.chatMessageBulk().enabled()) {
            chatMessageBulkScheduler.scheduleWithFixedDelay(
                    chatMessageBulkJob::run,
                    Duration.ofMillis(properties.chatMessageBulk().rateMillis())
            );
        }
    }
}
