package stack.moaticket.application.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import stack.moaticket.application.component.scheduler.JobSchedulerProperties;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {
    private final JobSchedulerProperties properties;

    private static final String SESSION_START_SCHEDULER_PREFIX = "sch-ss-";
    private static final String TICKET_RELEASE_SCHEDULER_PREFIX = "sch-tr-";

    private static final String SESSION_START_EXECUTOR_PREFIX = "ex-ss-";
    private static final String TICKET_RELEASE_EXECUTOR_PREFIX = "ex-tr-";

    private static final String CHAT_MESSAGE_BULK_SCHEDULER_PREFIX = "sch-cm-bulk-";

    @Bean(name = "sessionStartScheduler")
    public ThreadPoolTaskScheduler sessionStartScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix(SESSION_START_SCHEDULER_PREFIX);
        ts.setWaitForTasksToCompleteOnShutdown(true);
        ts.setAwaitTerminationSeconds(10);
        ts.initialize();

        return ts;
    }

    @Bean(name = "sessionStartExecutor")
    public ThreadPoolTaskExecutor sessionStartExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(properties.sessionStart().executor().maxThread());
        ex.setMaxPoolSize(properties.sessionStart().executor().maxThread());
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix(SESSION_START_EXECUTOR_PREFIX);
        ex.initialize();
        return ex;
    }

    @Bean(name = "ticketReleaseScheduler")
    public ThreadPoolTaskScheduler ticketReleaseScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix(TICKET_RELEASE_SCHEDULER_PREFIX);
        ts.setWaitForTasksToCompleteOnShutdown(true);
        ts.setAwaitTerminationSeconds(10);
        ts.initialize();

        return ts;
    }

    @Bean(name = "ticketReleaseExecutor")
    public ThreadPoolTaskExecutor ticketReleaseExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(properties.ticketRelease().executor().maxThread());
        ex.setMaxPoolSize(properties.ticketRelease().executor().maxThread());
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix(TICKET_RELEASE_EXECUTOR_PREFIX);
        ex.initialize();
        return ex;
    }

    @Bean(name = "chatMessageBulkScheduler")
    public ThreadPoolTaskScheduler chatMessageBulkScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix(CHAT_MESSAGE_BULK_SCHEDULER_PREFIX);
        ts.setWaitForTasksToCompleteOnShutdown(true);
        ts.setAwaitTerminationSeconds(10);
        ts.initialize();

        return ts;
    }
}
