package stack.moaticket.application.component.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.BDDMockito.then;

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(
        properties = {
                "app.server.scheduler.session-start=false",
                "app.server.scheduler.ticket-release=false"
        })
public class JobSchedulerDisableIT {
    @MockitoBean("sessionStartScheduler") TaskScheduler sessionStartScheduler;
    @MockitoBean("ticketReleaseScheduler") TaskScheduler ticketReleaseScheduler;

    @Test
    @DisplayName("JobScheduler는 Property가 꺼져있다면 아무것도 하지 않는다.")
    void disableRegistersJobs() {
        // then
        then(sessionStartScheduler).shouldHaveNoMoreInteractions();
        then(ticketReleaseScheduler).shouldHaveNoMoreInteractions();
    }
}
