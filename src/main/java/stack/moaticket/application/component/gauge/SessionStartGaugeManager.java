package stack.moaticket.application.component.gauge;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.LongSupplier;

import static stack.moaticket.application.component.gauge.SessionStartGaugeMetrics.*;

@Component
public class SessionStartGaugeManager {
    private final Counter throughput;
    private final Counter error;
    private final Timer dbPipeline;

    public SessionStartGaugeManager(MeterRegistry registry) {
        this.throughput = Counter.builder(SESSION_START_THROUGHPUT).register(registry);
        this.error = Counter.builder(SESSION_START_ERROR).register(registry);
        this.dbPipeline = Timer.builder(SESSION_START_PIPELINE)
                .publishPercentileHistogram(true)
                .register(registry);
    }

    public void recordDatabase(LongSupplier supplier) {
        try {
            long updated = dbPipeline.record(supplier);
            throughput.increment(updated);
        } catch (RuntimeException e) {
            error.increment();
            throw e;
        }
    }
}
