package stack.moaticket.system.alarm.sse.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import stack.moaticket.system.alarm.sse.component.gauge.SseGaugeBinder;
import stack.moaticket.system.alarm.sse.component.gauge.SseGaugeManager;
import stack.moaticket.system.alarm.sse.component.register.SseEmitterRegister;

@ConditionalOnProperty(
        value = "app.server.alarm.type",
        havingValue = "SSE",
        matchIfMissing = true
)
@Configuration
public class SseGaugeConfig {

    @Bean
    public SseGaugeBinder sseGaugeBinder(
            MeterRegistry meterRegistry,
            SseEmitterRegister sseEmitterRegister,
            @Qualifier("asyncExecutor") AsyncTaskExecutor asyncExecutor) {
        return new SseGaugeBinder(meterRegistry, sseEmitterRegister, asyncExecutor);
    }

    @Bean
    public SseGaugeManager sseGaugeManager(
            MeterRegistry meterRegistry) {
        return new SseGaugeManager(meterRegistry);
    }
}
