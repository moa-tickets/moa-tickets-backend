package stack.moaticket.system.sse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.sse.register.SseEmitterRegister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class SseSubscribeServiceTest {
    @Mock SseEmitterRegister sseEmitterRegister;
    @Mock private SseSendService sseSendService;

    @InjectMocks private SseSubscribeService sseSubscribeService;

    @Captor private ArgumentCaptor<SseEmitter> sseEmitterCaptor;

    @Test
    @DisplayName("구독 시 emitter를 등록하고 connect 이벤트를 1회 전송한 뒤 emitter를 반환한다.")
    void subscribeSuccess() {
        // given
        Long memberId = 1L;

        // when
        SseEmitter ret = sseSubscribeService.subscribe(memberId);

        // then
        InOrder inOrder = inOrder(sseEmitterRegister, sseSendService);
        inOrder.verify(sseEmitterRegister).insert(eq(memberId), sseEmitterCaptor.capture());

        SseEmitter sseEmitter = sseEmitterCaptor.getValue();
        inOrder.verify(sseSendService).sendOrThrow(eq(memberId), eq(sseEmitter), eq("connect"), isNull());

        assertThat(ret).isSameAs(sseEmitter);

        then(sseEmitterRegister).shouldHaveNoMoreInteractions();
        then(sseSendService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Connect 실패 시 emitter 등록을 롤백하고 예외를 전파한다.")
    void subscribeFailRollBackRegister() {
        // given
        Long memberId = 1L;

        willThrow(new MoaException(MoaExceptionType.SSE_ERROR))
                .given(sseSendService)
                .sendOrThrow(eq(memberId), any(SseEmitter.class), eq("connect"), isNull());

        // when
        assertThatThrownBy(() -> sseSubscribeService.subscribe(memberId))
                .isInstanceOf(MoaException.class);

        // then
        then(sseEmitterRegister).should().insert(eq(memberId), sseEmitterCaptor.capture());
        then(sseEmitterRegister).should().remove(eq(memberId), eq(sseEmitterCaptor.getValue()));
    }
}
