package stack.moaticket.application.component.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.server.scheduler")
public record JobSchedulerProperties(
        Config sessionStart,
        Config ticketRelease,
        Config chatMessageBulk) {
    public record Config(
            boolean enabled,
            Long batchSize,
            int rateMillis,
            Executor executor
    ) {
        public record Executor(
                int maxThread
        ) {}
    }
}
