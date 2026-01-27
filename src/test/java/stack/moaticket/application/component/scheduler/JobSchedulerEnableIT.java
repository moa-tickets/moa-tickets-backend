package stack.moaticket.application.component.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.mockito.BDDMockito.*;

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(
        properties = {
                "app.server.scheduler.session-start=true",
                "app.server.scheduler.ticket-release=true"
        })
public class JobSchedulerEnableIT {
    @MockitoBean("sessionStartScheduler") TaskScheduler sessionStartScheduler;
    @MockitoBean("ticketReleaseScheduler") TaskScheduler ticketReleaseScheduler;

    @Test
    @DisplayName("JobScheduler는 Property가 켜져있다면 Job을 등록한다.")
    void enableRegistersJobs() {
        // then
        then(sessionStartScheduler).should().scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(2)));
        then(ticketReleaseScheduler).should().scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(2)));
    }
}
