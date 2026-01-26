package stack.moaticket.system.alarm.sse.model;

import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

public class EmitterMeta {
    @Getter
    private final SseEmitter emitter;
    private final AtomicLong lastSentAtMillis;

    private static final int INTERVAL = 30_000;

    public EmitterMeta(SseEmitter emitter) {
        this.emitter = emitter;
        this.lastSentAtMillis = new AtomicLong(System.currentTimeMillis());
    }

    public boolean tryMarkHeartbeat(LocalDateTime currentTime) {
        long prev = lastSentAtMillis.get();
        long cur = convertToMillis(currentTime);
        if(cur - prev < INTERVAL) return false;
        else return lastSentAtMillis.compareAndSet(prev, cur);
    }

    public void updateLastSentAt(LocalDateTime currentSend) {
        long currentSendMillis = convertToMillis(currentSend);
        lastSentAtMillis.updateAndGet(prev -> Math.max(prev, currentSendMillis));
    }

    private long convertToMillis(LocalDateTime dateTime) {
        return dateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}
