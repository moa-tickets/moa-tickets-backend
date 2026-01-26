package stack.moaticket.system.alarm.sse.service;

import org.springframework.beans.factory.annotation.Qualifier;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.model.AlarmTarget;
import stack.moaticket.system.alarm.core.service.AlarmSendService;

import java.util.concurrent.Executor;

public class AsyncSseSendService implements AlarmSendService {
    private final AlarmSendService delegate;
    private final Executor executor;

    public AsyncSseSendService(
            @Qualifier("syncSseSendService") AlarmSendService delegate,
            @Qualifier("heartbeatExecutor") Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public void send(Long memberId, AlarmTarget target, AlarmMessage message) {
        executor.execute(() -> delegate.send(memberId, target, message));
    }

    @Override
    public void sendOrThrow(Long memberId, AlarmTarget target, AlarmMessage message) {
        executor.execute(() -> delegate.sendOrThrow(memberId, target, message));
    }

    @Override
    public void sendAll(Long memberId, AlarmMessage message) {
        executor.execute(() -> delegate.sendAll(memberId, message));
    }
}
