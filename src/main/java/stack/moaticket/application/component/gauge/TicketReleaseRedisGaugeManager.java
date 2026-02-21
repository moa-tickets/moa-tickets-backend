package stack.moaticket.application.component.gauge;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static stack.moaticket.application.component.gauge.TicketReleaseGaugeMetrics.*;

@Component
public class TicketReleaseRedisGaugeManager {
    private final Counter produced;
    private final Counter consumed;
    private final Counter redelivered;
    private final Counter error;

    private final Counter consumerLocked;
    private final Counter consumerUnlocked;
    private final Counter consumerAcked;
    private final Counter consumerDone;

    private final Counter pelLocked;
    private final Counter pelUnlocked;
    private final Counter pelAcked;
    private final Counter pelDone;
    private final Counter pelDropped;

    private final Timer producePipeline;
    private final Timer consumePipeline;
    private final Timer pelPipeline;

    public TicketReleaseRedisGaugeManager(MeterRegistry registry) {
        this.produced = Counter.builder(TICKET_RELEASE_REDIS_PRODUCED).register(registry);
        this.consumed = Counter.builder(TICKET_RELEASE_REDIS_CONSUMED).register(registry);
        this.redelivered = Counter.builder(TICKET_RELEASE_REDIS_REDELIVERED).register(registry);
        this.error = Counter.builder(TICKET_RELEASE_REDIS_ERROR).register(registry);

        this.consumerLocked = Counter.builder(TICKET_RELEASE_REDIS_CONSUMER_LOCKED).register(registry);
        this.consumerUnlocked = Counter.builder(TICKET_RELEASE_REDIS_CONSUMER_UNLOCKED).register(registry);
        this.consumerAcked = Counter.builder(TICKET_RELEASE_REDIS_CONSUMER_ACKED).register(registry);
        this.consumerDone = Counter.builder(TICKET_RELEASE_REDIS_CONSUMER_DONE).register(registry);

        this.pelLocked = Counter.builder(TICKET_RELEASE_REDIS_PEL_LOCKED).register(registry);
        this.pelUnlocked = Counter.builder(TICKET_RELEASE_REDIS_PEL_UNLOCKED).register(registry);
        this.pelAcked = Counter.builder(TICKET_RELEASE_REDIS_PEL_ACKED).register(registry);
        this.pelDone = Counter.builder(TICKET_RELEASE_REDIS_PEL_DONE).register(registry);
        this.pelDropped = Counter.builder(TICKET_RELEASE_REDIS_PEL_DROPPED).register(registry);

        this.producePipeline = Timer.builder(TICKET_RELEASE_REDIS_PRODUCE_PIPELINE)
                .publishPercentileHistogram(true)
                .register(registry);
        this.consumePipeline = Timer.builder(TICKET_RELEASE_REDIS_CONSUME_PIPELINE)
                .publishPercentileHistogram(true)
                .register(registry);
        this.pelPipeline = Timer.builder(TICKET_RELEASE_REDIS_PEL_PIPELINE)
                .publishPercentileHistogram(true)
                .register(registry);
    }

    public void recordProducer(Runnable action) {
        produced.increment();

        try {
            producePipeline.record(action);
        } catch (RuntimeException e) {
            error.increment();
            throw e;
        }
    }

    public void recordConsumer(Consumer<TicketReleaseMarks> action) {
        consumed.increment();
        TicketReleaseMarks marks = new TicketReleaseMarks();

        try {
            consumePipeline.record(() -> action.accept(marks));
        } catch (RuntimeException e) {
            error.increment();
            throw e;
        } finally {
            if(marks.isLocked()) consumerLocked.increment();
            if(marks.isUnlocked()) consumerUnlocked.increment();
            if(marks.isDone()) consumerDone.increment();
            if(marks.isAcked()) consumerAcked.increment();
        }
    }

    public void recordPel(Consumer<TicketReleaseMarks> action) {
        redelivered.increment();
        TicketReleaseMarks marks = new TicketReleaseMarks();

        try {
            pelPipeline.record(() -> action.accept(marks));
        } catch (RuntimeException e) {
            error.increment();
            throw e;
        } finally {
            if(marks.isLocked()) pelLocked.increment();
            if(marks.isUnlocked()) pelUnlocked.increment();
            if(marks.isDone()) pelDone.increment();
            if(marks.isAcked()) pelAcked.increment();
            if(marks.isDropped()) pelDropped.increment();
        }
    }
}
