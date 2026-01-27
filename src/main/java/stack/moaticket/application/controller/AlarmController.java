package stack.moaticket.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.application.dto.SubscribeTicketDto;
import stack.moaticket.application.dto.UnsubscribeTicketDto;
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

    @PostMapping("/ticket")
    public ResponseEntity<Void> subscribeTicket(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SubscribeTicketDto.Request request) {
        alarmService.subscribeTicketReleaseAlarm(memberId, request.getTicketId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/ticket")
    public ResponseEntity<Void> unsubscribeTicket(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UnsubscribeTicketDto.Request request) {
        alarmService.unsubscribeTicketReleaseAlarm(memberId, request.getTicketId(), request.getTicketAlarmId());
        return ResponseEntity.noContent().build();
    }
}
