package stack.moaticket.system.sse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.system.alarm.core.component.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.core.service.AlarmSendService;
import stack.moaticket.system.alarm.sse.service.AsyncSseSendService;

import java.util.concurrent.Executor;

import static org.mockito.BDDMockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class AsyncSseSendServiceTest {
    @Mock
    AlarmSendService delegate;

    Executor direct = Runnable::run;

    @Test
    @DisplayName("SseSendService 위임 테스트.")
    void sendExecutesDelegate() {
        // given
        AsyncSseSendService async = new AsyncSseSendService(delegate, direct);

        // when
        async.send(1L, new AlarmTarget("c1"), AlarmMessageFactory.heartbeat());

        // then
        then(delegate).should().send(eq(1L), any(AlarmTarget.class), any(AlarmMessage.class));
    }
}
