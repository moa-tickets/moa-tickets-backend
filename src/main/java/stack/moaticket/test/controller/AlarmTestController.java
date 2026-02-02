package stack.moaticket.test.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.test.service.AlarmTestService;

@RequestMapping("/test/alarm")
@RequiredArgsConstructor
public class AlarmTestController {
    private final AlarmTestService alarmTestService;

    @GetMapping(value = "/sub", produces = "text/event-stream")
    public SseEmitter subscribe(
            @RequestHeader("X-LoadTest-Id") String memberId) {
        return alarmTestService.subscribe(Long.parseLong(memberId));
    }
}
