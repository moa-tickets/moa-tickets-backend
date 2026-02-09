package stack.moaticket.system.sse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;
import stack.moaticket.system.alarm.sse.service.SseHeartbeatService;
import stack.moaticket.system.alarm.sse.service.SseSendService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class SseHeartbeatServiceTest {
    @Mock SseEmitterRegister sseEmitterRegister;
    @Mock SseSendService sseSendService;
    @Mock(name = "asyncExecutor") Executor asyncExecutor;

    @InjectMocks SseHeartbeatService sseHeartbeatService;

    @Test
    @DisplayName("SendHeartbeat는 getFiltered() 결과의 모든 cid에 Heartbeat를 전송한다.")
    void sendHeartbeatSendsToAllFilteredReceivers() {
        // given
        Map<Integer, List<EmitterMeta>> receivers = new HashMap<>();

        EmitterMeta m1 = mock(EmitterMeta.class);
        EmitterMeta m2 = mock(EmitterMeta.class);
        EmitterMeta m3 = mock(EmitterMeta.class);

        receivers.put(1, List.of(m1, m2));
        receivers.put(2, List.of(m3));

        given(m1.getMemberId()).willReturn(1L);
        given(m2.getMemberId()).willReturn(2L);
        given(m3.getMemberId()).willReturn(3L);

        given(m1.getConnectionId()).willReturn("c1");
        given(m2.getConnectionId()).willReturn("c2");
        given(m3.getConnectionId()).willReturn("c3");

        given(sseEmitterRegister.getFilteredForShard(any()))
                .willReturn(receivers);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(asyncExecutor).execute(any(Runnable.class));

        // when
        sseHeartbeatService.sendHeartbeat();

        // then
        then(sseEmitterRegister).should(times(1)).getFilteredForShard(any());

        ArgumentCaptor<AlarmTarget> targetCaptor = ArgumentCaptor.forClass(AlarmTarget.class);

        then(sseSendService).should(times(3))
                .sendOrThrow(anyLong(), targetCaptor.capture(), any(AlarmMessage.class));

        assertThat(targetCaptor.getAllValues())
                .extracting(AlarmTarget::connectionId)
                .containsExactlyInAnyOrder("c1", "c2", "c3");
        then(sseSendService).should().sendOrThrow(eq(1L), any(AlarmTarget.class), any(AlarmMessage.class));
        then(sseSendService).should().sendOrThrow(eq(2L), any(AlarmTarget.class), any(AlarmMessage.class));
        then(sseSendService).should().sendOrThrow(eq(3L), any(AlarmTarget.class), any(AlarmMessage.class));
    }

    @Test
    @DisplayName("GET_FILTERED 결과가 비어있으면 전송하지 않는다.")
    void sendHeartbeatIfNoReceiversDoNotSend() {
        // given
        given(sseEmitterRegister.getFilteredForShard(any()))
                .willReturn(Collections.emptyMap());

        // when
        sseHeartbeatService.sendHeartbeat();

        // then
        then(sseSendService).shouldHaveNoInteractions();
    }
}
