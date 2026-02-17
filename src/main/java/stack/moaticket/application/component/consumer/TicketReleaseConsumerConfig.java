package stack.moaticket.application.component.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AlarmConsumerProperties.class)
public class TicketReleaseConsumerConfig {
    private final AlarmConsumerProperties properties;

    private static final String ALARM_CONSUMER_EXECUTOR_PREFIX = "ex-al-consumer";

    @Bean(name = "ticketReleaseRedisConsumerExecutor")
    public ThreadPoolTaskExecutor ticketReleaseRedisConsumerExecutor() {
        int totalThread = properties.ticketRelease().consumerThread() + properties.ticketRelease().pelThread();

        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(totalThread);
        ex.setMaxPoolSize(totalThread);
        ex.setQueueCapacity(0);
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(10);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.setThreadNamePrefix(ALARM_CONSUMER_EXECUTOR_PREFIX);
        ex.initialize();
        return ex;
    }
}
