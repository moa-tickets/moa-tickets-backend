package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.application.dto.SeatAlarmReserve;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.member.entity.Member;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {
    private final AlarmService alarmService;

    @GetMapping(value = "/sub", produces = "text/event-stream")
    public SseEmitter subscribe(
            @AuthenticationPrincipal Member member) {
        System.out.println("멤버 아이디: " + member.getId());
        return alarmService.subscribe(member);
    }
}
