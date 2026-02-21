package stack.moaticket.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class ChatAsyncConfig {
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
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(5000);
        ex.setThreadNamePrefix("ex-buffer-to-db-async-");
        ex.initialize();
        return ex;
    }
}
