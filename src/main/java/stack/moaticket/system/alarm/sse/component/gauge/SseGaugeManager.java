package stack.moaticket.system.alarm.sse.component.gauge;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static stack.moaticket.system.alarm.sse.component.gauge.SseGaugeMetrics.*;

public class SseGaugeManager {
    private final Counter attempt;
    private final Counter fail;
    private final Timer timer;

    public SseGaugeManager(MeterRegistry registry) {
        this.attempt = Counter.builder(SSE_SEND_ATTEMPT_COUNTER).register(registry);
        this.fail = Counter.builder(SSE_SEND_FAILURE_COUNTER).register(registry);
        this.timer = Timer.builder(SSE_SEND_SECONDS)
                .publishPercentileHistogram(true)
                .register(registry);
    }

    public void recordSend(Consumer<Runnable> actionWhenFail) {
        attempt.increment();

        AtomicBoolean failed = new AtomicBoolean(false);
        Runnable markFailed = () -> failed.set(true);

        timer.record(() -> actionWhenFail.accept(markFailed));

        if(failed.get()) {
            fail.increment();
        }
    }
}
