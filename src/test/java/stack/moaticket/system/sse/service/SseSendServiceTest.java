package stack.moaticket.system.sse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.alarm.sse.service.SseSendService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class SseSendServiceTest {
    @Mock private SseEmitterRegister sseEmitterRegister;

    @InjectMocks private SseSendService sseSendService;

    @Test
    @DisplayName("단일 대상으로 예외 없이 전송을 성공한다.")
    void sendSuccess() throws Exception {
        // given
        Long mid = 1L;
        String cid = "c1";
        AlarmTarget target = new AlarmTarget(cid);
        AlarmMessage message = mock(AlarmMessage.class);

        given(message.key()).willReturn("type");
        given(message.payload()).willReturn("payload");

        SseEmitter emitter = mock(SseEmitter.class);
        EmitterMeta meta = new EmitterMeta(emitter);

        given(sseEmitterRegister.get(mid, cid)).willReturn(meta);

        // when
        sseSendService.send(mid, target, message);

        // then
        then(sseEmitterRegister).should().get(mid, cid);
        then(emitter).should(times(1)).send(any(SseEmitter.SseEventBuilder.class));
        then(sseEmitterRegister).should(never()).remove(anyLong(), anyString());
        then(emitter).should(never()).completeWithError(any());
    }

    @Test
    @DisplayName("단일 대상으로 전송을 실패하지만 예외는 무시한다.")
    void sendFailButIgnoredException() throws Exception {
        // given
        Long mid = 1L;
        String cid = "c1";
        AlarmTarget target = new AlarmTarget(cid);
        AlarmMessage message = mock(AlarmMessage.class);

        given(message.key()).willReturn("type");
        given(message.payload()).willReturn("payload");

        SseEmitter emitter = mock(SseEmitter.class);
        EmitterMeta meta = new EmitterMeta(emitter);

        IOException cause = new IOException("boom");
        willThrow(cause).given(emitter).send(any(SseEmitter.SseEventBuilder.class));
        given(sseEmitterRegister.get(mid, cid)).willReturn(meta);

        // when
        sseSendService.send(mid, target, message);

        // then
        then(sseEmitterRegister).should().remove(mid, cid);
        then(emitter).should().completeWithError(cause);
    }

    @Test
    @DisplayName("단일 대상으로 전송을 실패하면 예외를 전파한다.")
    void sendFailAndThrowException() throws Exception {
        // given
        Long mid = 1L;
        String cid = "c1";
        AlarmTarget target = new AlarmTarget(cid);
        AlarmMessage message = mock(AlarmMessage.class);

        given(message.key()).willReturn("type");
        given(message.payload()).willReturn("payload");

        SseEmitter emitter = mock(SseEmitter.class);
        EmitterMeta meta = new EmitterMeta(emitter);

        given(sseEmitterRegister.get(mid, cid)).willReturn(meta);
        doThrow(new IllegalStateException("closed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // when & then
        assertThatThrownBy(() -> sseSendService.sendOrThrow(mid, target, message))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.SSE_ERROR);

        then(sseEmitterRegister).should().remove(mid, cid);
        then(emitter).should().completeWithError(any(IllegalStateException.class));
    }

    @Test
    @DisplayName("Emitter 리스트가 비어있는 경우 전송을 시도하지 않는다.")
    void sendAllNullListShouldReturn() {
        // given
        Long mid = 1L;
        AlarmMessage message = mock(AlarmMessage.class);

        given(sseEmitterRegister.getSseEmitters(mid)).willReturn(Collections.emptyMap());

        // when
        sseSendService.sendAll(mid, message);

        // then
        then(sseEmitterRegister).should().getSseEmitters(mid);
        then(sseEmitterRegister).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("전송 리스트 중에 일부가 실패하더라도 계속 이어서 처리한다.")
    void sendAllMixedResultShouldContinue() throws Exception {
        // given
        Long mid = 1L;
        AlarmMessage message = mock(AlarmMessage.class);

        given(message.key()).willReturn("type");
        given(message.payload()).willReturn("payload");

        String failCid = "c_fail";
        String succeedCid = "c_succeed";

        SseEmitter fail = mock(SseEmitter.class);
        SseEmitter succeed = mock(SseEmitter.class);

        EmitterMeta failMeta = new EmitterMeta(fail);
        EmitterMeta succeedMeta = new EmitterMeta(succeed);

        willThrow(new IOException("fail"))
                .given(fail).send(any(SseEmitter.SseEventBuilder.class));
        given(sseEmitterRegister.getSseEmitters(mid))
                .willReturn(Map.of(failCid, failMeta, succeedCid, succeedMeta));

        // when
        sseSendService.sendAll(mid, message);

        // then
        then(fail).should().send(any(SseEmitter.SseEventBuilder.class));
        then(succeed).should().send(any(SseEmitter.SseEventBuilder.class));

        then(sseEmitterRegister).should().remove(mid, failCid);
        then(fail).should().completeWithError(any(IOException.class));

        then(sseEmitterRegister).should(never()).remove(mid, succeedCid);
        then(succeed).should(never()).completeWithError(any());
    }
}
