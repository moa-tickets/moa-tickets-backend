package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.application.service.AlarmService;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {
    private final AlarmService alarmService;

    @GetMapping(value = "/sub", produces = "text/event-stream")
    public SseEmitter subscribe(
            @AuthenticationPrincipal Long memberId) {
        return alarmService.subscribe(memberId);
    }
}
