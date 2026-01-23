package stack.moaticket.application.port;

public interface AlarmSender {
    void sendAll(Long memberId, AlarmMessage alarmMessage);
}
