package stack.moaticket.application.component.gauge;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.LongSupplier;

import static stack.moaticket.application.component.gauge.TicketReleaseGaugeMetrics.*;

@Component
public class TicketReleaseExecutorGaugeManager {
    private final Counter throughput;
    private final Counter error;
    private final Timer pipeline;

    public TicketReleaseExecutorGaugeManager(MeterRegistry registry) {
        this.throughput = Counter.builder(TICKET_RELEASE_THROUGHPUT).register(registry);
        this.error = Counter.builder(TICKET_RELEASE_ERROR).register(registry);
        this.pipeline = Timer.builder(TICKET_RELEASE_PIPELINE)
                .publishPercentileHistogram(true)
                .register(registry);
    }

    public void recordDatabase(LongSupplier supplier) {
        try {
            long updated = pipeline.record(supplier);
            throughput.increment(updated);
        } catch (RuntimeException e) {
            error.increment();
            throw e;
        }
    }
}
