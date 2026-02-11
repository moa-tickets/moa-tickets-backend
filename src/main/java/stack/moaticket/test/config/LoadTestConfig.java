package stack.moaticket.test.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stack.moaticket.application.job.ConcertStartInformJob;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;
import stack.moaticket.system.jwt.JwtUtil;
import stack.moaticket.test.service.AlarmTestService;
import stack.moaticket.test.service.LoadTestAuthService;

@EnableConfigurationProperties(LoadTestProperties.class)
@ConditionalOnProperty(
        value = "app.server.test.load-test.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Configuration
public class LoadTestConfig {

    @Bean
    public AlarmTestService alarmTestService(
            SseSubscribeService sseSubscribeService,
            ConcertStartInformJob concertStartInformJob) {
        return new AlarmTestService(sseSubscribeService, concertStartInformJob);
    }

    @Bean
    public LoadTestAuthService loadTestAuthService(JwtUtil jwtUtil) {
        return new LoadTestAuthService(jwtUtil);
    }
}
