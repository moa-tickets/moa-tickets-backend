package stack.moaticket.system.sse.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.system.alarm.sse.job.SseHeartbeatInformJob;
import stack.moaticket.system.alarm.sse.service.SseHeartbeatService;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class SseHeartbeatInformJobTest {
    @Mock SseHeartbeatService sseHeartbeatService;

    @InjectMocks SseHeartbeatInformJob job;

    @Test
    @DisplayName("RUN은 Heartbeat 전송을 호출한다.")
    void runCallsService() {
        // when
        job.run();

        // then
        then(sseHeartbeatService).should(times(1)).sendHeartbeat();
    }
}