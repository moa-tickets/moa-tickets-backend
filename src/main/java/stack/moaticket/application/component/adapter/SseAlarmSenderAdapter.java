package stack.moaticket.application.component.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import stack.moaticket.application.port.AlarmMessage;
import stack.moaticket.application.port.AlarmSender;
import stack.moaticket.system.sse.service.SseSendService;

@ConditionalOnProperty(
        name = "alarm.sender",
        havingValue = "sse",
        matchIfMissing = true
)
@Component
@RequiredArgsConstructor
public class SseAlarmSenderAdapter implements AlarmSender {
    private final SseSendService sseSendService;


    @Override
    public void sendAll(Long memberId, AlarmMessage alarmMessage) {
        sseSendService.sendAll(memberId, alarmMessage.key(), alarmMessage.payload());
    }
}
