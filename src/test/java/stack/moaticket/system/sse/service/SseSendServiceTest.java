package stack.moaticket.system.sse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.system.sse.register.SseEmitterRegister;

@ExtendWith(MockitoExtension.class)
public class SseSendServiceTest {
    @Mock private SseEmitterRegister sseEmitterRegister;

    @InjectMocks private SseSendService sseSendService;

    @Test
    @DisplayName("")
    void test() {
        // given

        // when

        // then
    }
}
