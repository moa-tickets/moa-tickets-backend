package stack.moaticket.application.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import stack.moaticket.application.component.consumer.ChatBulkProperties;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class ChatAsyncConfig {

    private final ChatBulkProperties properties;

    @Bean(name = "chatToBufferExecutor")
    public Executor chatToBufferExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(10);
        ex.setMaxPoolSize(10);
        ex.setQueueCapacity(5000);
        ex.setThreadNamePrefix("ex-chat-to-buffer-async-");
        ex.initialize();
        return ex;
    }

    @Bean(name = "bufferToDbExecutor")
    public ThreadPoolTaskExecutor bufferToDbExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(properties.chatMessageBulk().shardCount());
        ex.setMaxPoolSize(properties.chatMessageBulk().shardCount());
        ex.setQueueCapacity(5000);
        ex.setThreadNamePrefix("ex-buffer-to-db-async-");
        ex.initialize();
        return ex;
    }
}
