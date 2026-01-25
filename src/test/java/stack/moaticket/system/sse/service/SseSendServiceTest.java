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
        given(sseEmitterRegister.get(mid, cid)).willReturn(emitter);

        // when
        sseSendService.send(mid, target, message);

        // then
        verify(sseEmitterRegister).get(mid, cid);
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(sseEmitterRegister, never()).remove(anyLong(), anyString());
        verify(emitter, never()).completeWithError(any());
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
        IOException cause = new IOException("boom");

        given(sseEmitterRegister.get(mid, cid)).willReturn(emitter);
        doThrow(cause).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // when
        sseSendService.send(mid, target, message);

        // then
        verify(sseEmitterRegister).remove(mid, cid);
        verify(emitter).completeWithError(cause);
    }

    @Test
    @DisplayName("단일 대상으로 전송을 실패하고, 예외가 발생한다.")
    void sendFailAndThrowException() throws Exception {
        // given
        Long mid = 1L;
        String cid = "c1";
        AlarmTarget target = new AlarmTarget(cid);
        AlarmMessage message = mock(AlarmMessage.class);

        given(message.key()).willReturn("type");
        given(message.payload()).willReturn("payload");

        SseEmitter emitter = mock(SseEmitter.class);

        given(sseEmitterRegister.get(mid, cid)).willReturn(emitter);
        doThrow(new IllegalStateException("closed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // when & then
        assertThatThrownBy(() -> sseSendService.sendOrThrow(mid, target, message))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.SSE_ERROR);

        verify(sseEmitterRegister).remove(mid, cid);
        verify(emitter).completeWithError(any(IllegalStateException.class));
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
        verify(sseEmitterRegister).getSseEmitters(mid);
        verifyNoMoreInteractions(sseEmitterRegister);
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

        doThrow(new IOException("fail"))
                .when(fail).send(any(SseEmitter.SseEventBuilder.class));
        given(sseEmitterRegister.getSseEmitters(mid))
                .willReturn(Map.of(failCid, fail, succeedCid, succeed));
        given(sseEmitterRegister.get(mid, failCid)).willReturn(fail);
        given(sseEmitterRegister.get(mid, succeedCid)).willReturn(succeed);


        // when
        sseSendService.sendAll(mid, message);

        // then
        verify(fail).send(any(SseEmitter.SseEventBuilder.class));
        verify(succeed).send(any(SseEmitter.SseEventBuilder.class));

        verify(sseEmitterRegister).remove(mid, failCid);
        verify(fail).completeWithError(any(IOException.class));

        verify(sseEmitterRegister, never()).remove(mid, succeedCid);
        verify(succeed, never()).completeWithError(any());
    }
}
