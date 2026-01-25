package stack.moaticket.system.alarm.core.service;

import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;

public interface AlarmSendService {
    void send(Long memberId, AlarmTarget target, AlarmMessage message);
    void sendOrThrow(Long memberId, AlarmTarget target, AlarmMessage message);
    void sendAll(Long memberId, AlarmMessage message);
}
