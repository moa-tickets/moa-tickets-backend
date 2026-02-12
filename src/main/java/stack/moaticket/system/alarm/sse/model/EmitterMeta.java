package stack.moaticket.system.alarm.sse.model;

import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class EmitterMeta {
    @Getter
    private final Long memberId;
    @Getter
    private final String connectionId;
    @Getter
    private final SseEmitter emitter;
    private final AtomicLong lastSentAtMillis;
    private final AtomicBoolean isAlive = new AtomicBoolean(true);

    private static final int INTERVAL = 30_000;

    public EmitterMeta(Long memberId, String connectionId, SseEmitter emitter) {
        this.memberId = memberId;
        this.connectionId = connectionId;
        this.emitter = emitter;
        this.lastSentAtMillis = new AtomicLong(System.currentTimeMillis());
    }

    public boolean tryMarkHeartbeat(long cur) {
        long prev = lastSentAtMillis.get();
        if(cur - prev < INTERVAL) return false;
        else return lastSentAtMillis.compareAndSet(prev, cur);
    }

    public void updateLastSentAt(long cur) {
        lastSentAtMillis.updateAndGet(prev -> Math.max(prev, cur));
    }

    public boolean isAlive() {
        return isAlive.get();
    }

    public boolean markDead() {
        return isAlive.compareAndSet(true, false);
    }
}
