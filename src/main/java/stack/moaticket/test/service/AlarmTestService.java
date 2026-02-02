package stack.moaticket.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;

@RequiredArgsConstructor
public class AlarmTestService {
    private final SseSubscribeService sseSubscribeService;

    public SseEmitter subscribe(Long memberId) {
        return sseSubscribeService.subscribe(memberId);
    }
}
