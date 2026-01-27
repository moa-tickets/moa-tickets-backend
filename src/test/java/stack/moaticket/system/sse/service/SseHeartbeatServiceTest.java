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
import stack.moaticket.system.alarm.sse.service.AsyncSseSendService;
import stack.moaticket.system.alarm.sse.service.SseHeartbeatService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class SseHeartbeatServiceTest {
    @Mock SseEmitterRegister sseEmitterRegister;
    @Mock AsyncSseSendService asyncSseSendService;

    @InjectMocks SseHeartbeatService sseHeartbeatService;

    @Test
    @DisplayName("SendHeartbeat는 getFiltered() 결과의 모든 cid에 Heartbeat를 전송한다.")
    void sendHeartbeatSendsToAllFilteredReceivers() {
        // given
        Map<Long, Map<String, EmitterMeta>> receivers = new HashMap<>();

        receivers.put(1L, Map.of(
                "c1", mock(EmitterMeta.class),
                "c2", mock(EmitterMeta.class)
        ));
        receivers.put(2L, Map.of(
                "c3", mock(EmitterMeta.class)
        ));

        given(sseEmitterRegister.getFiltered(any()))
                .willReturn(receivers);

        // when
        sseHeartbeatService.sendHeartbeat();

        // then
        then(sseEmitterRegister).should(times(1)).getFiltered(any());

        ArgumentCaptor<AlarmTarget> targetCaptor = ArgumentCaptor.forClass(AlarmTarget.class);

        then(asyncSseSendService).should(times(3))
                .sendOrThrow(anyLong(), targetCaptor.capture(), any(AlarmMessage.class));

        assertThat(targetCaptor.getAllValues())
                .extracting(AlarmTarget::connectionId)
                .containsExactlyInAnyOrder("c1", "c2", "c3");
        then(asyncSseSendService).should(times(2)).sendOrThrow(eq(1L), any(AlarmTarget.class), any(AlarmMessage.class));
        then(asyncSseSendService).should().sendOrThrow(eq(2L), any(AlarmTarget.class), any(AlarmMessage.class));
    }

    @Test
    @DisplayName("GET_FILTERED 결과가 비어있으면 전송하지 않는다.")
    void sendHeartbeatIfNoReceiversDoNotSend() {
        // given
        given(sseEmitterRegister.getFiltered(any()))
                .willReturn(Collections.emptyMap());

        // when
        sseHeartbeatService.sendHeartbeat();

        // then
        then(asyncSseSendService).shouldHaveNoInteractions();
    }
}
