package stack.moaticket.system.alarm.sse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import stack.moaticket.system.alarm.core.config.AlarmConfigProperties;
import stack.moaticket.system.alarm.sse.component.gauge.SseGaugeManager;
import stack.moaticket.system.alarm.sse.job.SseHeartbeatInformJob;
import stack.moaticket.system.alarm.sse.component.scheduler.SseHeartbeatScheduler;
import stack.moaticket.system.alarm.sse.component.register.SseEmitterRegister;
import stack.moaticket.system.alarm.sse.service.SseHeartbeatService;
import stack.moaticket.system.alarm.sse.service.SseSendService;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;

@ConditionalOnProperty(
        value = "app.server.alarm.type",
        havingValue = "SSE",
        matchIfMissing = true
)
@Configuration
@RequiredArgsConstructor
public class SseConfig {
    private static final String HEART_BEAT_SCHEDULER_PREFIX = "sch-hb-";
    private static final String SEND_EXECUTOR_PREFIX = "ex-sd-";
    private final AlarmConfigProperties properties;

    @Bean(name = "heartbeatInformScheduler")
    public ThreadPoolTaskScheduler heartbeatInformScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix(HEART_BEAT_SCHEDULER_PREFIX);
        ts.setWaitForTasksToCompleteOnShutdown(true);
        ts.setAwaitTerminationSeconds(10);
        ts.initialize();

        return ts;
    }

    @Bean(name = "asyncExecutor")
    public ThreadPoolTaskExecutor asyncExecutor() {
        int corePoolSize = properties.executor().coreThread();
        int maxPoolSize = properties.executor().maxThread();
        int queueCapacity = properties.executor().queueCapacity();

        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(corePoolSize);
        ex.setMaxPoolSize(maxPoolSize); // 테스트하려는 동시 접속자 수보다 넉넉하게
        ex.setQueueCapacity(queueCapacity); // 순간적인 몰림을 방지하는 완충 지대
        ex.setThreadNamePrefix(SEND_EXECUTOR_PREFIX);
        ex.initialize();
        return ex;
    }

    @Bean
    public SseEmitterRegister sseEmitterRegister() {
        return new SseEmitterRegister();
    }

    @Bean
    public SseHeartbeatScheduler sseHeartbeatInformScheduler(
            @Qualifier("heartbeatInformScheduler") TaskScheduler heartbeatScheduler,
            SseHeartbeatInformJob sseHeartbeatInformJob) {
        return new SseHeartbeatScheduler(heartbeatScheduler, sseHeartbeatInformJob);
    }

    @Bean
    public SseHeartbeatInformJob sseHeartbeatInformJob(
            SseHeartbeatService sseHeartbeatService) {
        return new SseHeartbeatInformJob(sseHeartbeatService);
    }

    @Bean
    public SseSubscribeService sseSubscribeService(
            SseEmitterRegister sseEmitterRegister,
            SseSendService sseSendService) {
        return new SseSubscribeService(sseEmitterRegister, sseSendService);
    }

    @Bean
    public SseSendService sseSendService(
            SseEmitterRegister register,
            @Qualifier("asyncExecutor") ThreadPoolTaskExecutor asyncExecutor,
            SseGaugeManager manager) {
        return new SseSendService(register, asyncExecutor, manager);
    }

    @Bean
    public SseHeartbeatService sseHeartbeatService(
            SseSendService sseSendService) {
        return new SseHeartbeatService(sseSendService);
    }
}
