package stack.moaticket.application.component.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.server.scheduler")
public record JobSchedulerProperties(
        boolean sessionStart,
        boolean ticketRelease
) {}
