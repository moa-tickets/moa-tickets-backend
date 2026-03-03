package stack.moaticket.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.application.job.ConcertStartInformJob;
import stack.moaticket.application.job.TicketReleaseInformJob;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;

@RequiredArgsConstructor
public class AlarmTestService {
    private final SseSubscribeService sseSubscribeService;
    private final ConcertStartInformJob concertStartInformJob;
    private final TicketReleaseInformJob ticketReleaseInformJob;

    public SseEmitter subscribe(Long memberId) {
        return sseSubscribeService.subscribe(memberId);
    }

    public void triggerSessionStartAlarm() {
        concertStartInformJob.runEpoch();
    }

    public void triggerTicketReleaseAlarm() { ticketReleaseInformJob.runEpoch(); }
}
