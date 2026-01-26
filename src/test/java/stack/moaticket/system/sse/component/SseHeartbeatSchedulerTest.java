package stack.moaticket.system.sse.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import stack.moaticket.system.alarm.sse.component.SseHeartbeatScheduler;
import stack.moaticket.system.alarm.sse.job.SseHeartbeatInformJob;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class SseHeartbeatSchedulerTest {
    @Mock TaskScheduler scheduler;
    @Mock SseHeartbeatInformJob job;

    @InjectMocks SseHeartbeatScheduler sseHeartbeatScheduler;

    @Test
    @DisplayName("START는 30초 FixedRate로 job.run()을 스케줄링한다.")
    void startSchedulesAtFixedRate() {
        // given
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        // when
        sseHeartbeatScheduler.start();

        // then
        then(scheduler).should(times(1))
                .scheduleAtFixedRate(runnableCaptor.capture(), durationCaptor.capture());

        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofSeconds(30));

        runnableCaptor.getValue().run();
        then(job).should(times(1)).run();
    }
}
