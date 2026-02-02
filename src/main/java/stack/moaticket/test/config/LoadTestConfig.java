package stack.moaticket.test.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;
import stack.moaticket.test.controller.AlarmTestController;
import stack.moaticket.test.service.AlarmTestService;

@ConditionalOnProperty(
        value = "app.server.test.load-test.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Configuration
public class LoadTestConfig {

    @Bean
    public AlarmTestController alarmTestController(AlarmTestService alarmTestService) {
        return new AlarmTestController(alarmTestService);
    }

    @Bean
    public AlarmTestService alarmTestService(SseSubscribeService sseSubscribeService) {
        return new AlarmTestService(sseSubscribeService);
    }
}
