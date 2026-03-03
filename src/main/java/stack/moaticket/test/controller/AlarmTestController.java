package stack.moaticket.test.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.test.service.AlarmTestService;

@ConditionalOnProperty(
        value = "app.server.test.load-test.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RestController
@RequestMapping("/test/alarm")
@RequiredArgsConstructor
public class AlarmTestController {
    private final AlarmTestService alarmTestService;

    @GetMapping(value = "/sub", produces = "text/event-stream")
    public SseEmitter subscribe(
            @RequestHeader("X-LoadTest-Id") String memberId) {
        return alarmTestService.subscribe(Long.parseLong(memberId));
    }

    @PostMapping("/trigger/session_start")
    public ResponseEntity<Void> triggerSessionStartAlarm() {
        alarmTestService.triggerSessionStartAlarm();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trigger/ticket_release")
    public ResponseEntity<Void> triggerTicketReleaseAlarm() {
        alarmTestService.triggerTicketReleaseAlarm();
        return ResponseEntity.noContent().build();
    }
}
