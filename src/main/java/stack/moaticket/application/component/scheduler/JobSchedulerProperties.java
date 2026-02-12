package stack.moaticket.application.component.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.server.scheduler")
public record JobSchedulerProperties(
        SessionStart sessionStart,
        TicketRelease ticketRelease) {
    public record SessionStart(
            boolean enabled,
            Long batchSize,
            int loopCount,
            int delay
    ) {}
    public record TicketRelease(
            boolean enabled,
            Long batchSize,
            int pageLimit,
            int loopCount,
            int delay
    ) {}
}
