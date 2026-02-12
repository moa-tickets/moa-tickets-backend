package stack.moaticket.system.sse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.core.model.ConnectPayload;
import stack.moaticket.system.alarm.sse.service.SseSendService;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.alarm.sse.component.register.SseEmitterRegister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class SseSubscribeServiceTest {
    @Mock SseEmitterRegister sseEmitterRegister;
    @Mock private SseSendService sseSendService;

    @InjectMocks private SseSubscribeService sseSubscribeService;

    @Test
    @DisplayName("구독 시 emitter를 등록하고 connect 이벤트를 1회 전송한 뒤 emitter를 반환한다.")
    void subscribeSuccess() {
        // given
        Long mid = 1L;
        String cid = "c1";

        given(sseEmitterRegister.insert(eq(mid), any(SseEmitter.class)))
                .willReturn(cid);

        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
        ArgumentCaptor<AlarmTarget> targetCaptor = ArgumentCaptor.forClass(AlarmTarget.class);
        ArgumentCaptor<AlarmMessage> messageCaptor = ArgumentCaptor.forClass(AlarmMessage.class);

        // when
        SseEmitter emitter = sseSubscribeService.subscribe(mid);

        // then
        InOrder inOrder = inOrder(sseEmitterRegister, sseSendService);
        inOrder.verify(sseEmitterRegister).insert(eq(mid), emitterCaptor.capture());
        inOrder.verify(sseSendService).sendOrThrow(eq(mid), targetCaptor.capture(), messageCaptor.capture());
        inOrder.verifyNoMoreInteractions();

        assertThat(emitter).isSameAs(emitterCaptor.getValue());
        assertThat(targetCaptor.getValue().connectionId()).isEqualTo(cid);
        assertThat(messageCaptor.getValue().getKey()).isEqualTo("CONNECT");
        assertThat(messageCaptor.getValue().getPayload().payload()).isEqualTo(new ConnectPayload(cid));

        then(sseEmitterRegister).shouldHaveNoMoreInteractions();
        then(sseSendService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Connect 실패 시 emitter 등록을 롤백하고 예외를 전파한다.")
    void subscribeFailRollBackRegister() {
        // given
        Long mid = 1L;
        String cid = "c1";

        given(sseEmitterRegister.insert(eq(mid), any(SseEmitter.class)))
                .willReturn(cid);

        willThrow(new MoaException(MoaExceptionType.SSE_ERROR))
                .given(sseSendService)
                .sendOrThrow(eq(mid), any(AlarmTarget.class), any(AlarmMessage.class));

        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);

        // when
        assertThatThrownBy(() -> sseSubscribeService.subscribe(mid))
                .isInstanceOf(MoaException.class);

        // then
        then(sseEmitterRegister).should().insert(eq(mid), emitterCaptor.capture());
        then(sseEmitterRegister).should().remove(eq(mid), eq(cid));

        then(sseEmitterRegister).shouldHaveNoMoreInteractions();
    }
}
