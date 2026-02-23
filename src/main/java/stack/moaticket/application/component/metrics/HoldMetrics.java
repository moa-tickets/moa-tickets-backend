package stack.moaticket.application.component.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class HoldMetrics {

    private final MeterRegistry registry;

    public HoldMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    private Timer timer(String name, String result) {
        return Timer.builder(name)
                .description("HOLD pipeline timing")
                .tag("result", result)
                .publishPercentileHistogram(true)  // Prometheus bucket 생성
                .register(registry);
    }

    public <T> T record(String timerName, String result, Callable<T> callable) {
        try {
            return timer(timerName, result).recordCallable(callable);
        } catch (RuntimeException e) {
            // error로도 찍고 다시 던지기
            timer(timerName, "error").record(() -> { /* no-op */ });
            throw e;
        } catch (Exception e) {
            timer(timerName, "error").record(() -> { /* no-op */ });
            throw new RuntimeException(e);
        }
    }

    public void record(String timerName, String result, Runnable runnable) {
        try {
            timer(timerName, result).record(runnable);
        } catch (RuntimeException e) {
            timer(timerName, "error").record(() -> { /* no-op */ });
            throw e;
        }
    }

    public static final class Names {
        public static final String HOLD_TOTAL = "booking_hold_total";
        public static final String HOLD_COUNT_SOLD = "booking_hold_count_sold";
        public static final String HOLD_UPDATE = "booking_hold_update";
        private Names() {}
    }

    public Timer.Sample start() {
        return Timer.start(registry);
    }

    public void stop(Timer.Sample sample, String timerName, String result) {
        Timer.builder(timerName)
                .tag("result", result)
                .publishPercentileHistogram(true)
                .register(registry);
        sample.stop(timer(timerName, result));
    }
}