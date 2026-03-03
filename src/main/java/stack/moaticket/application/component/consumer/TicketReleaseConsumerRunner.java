package stack.moaticket.application.component.consumer;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import stack.moaticket.application.component.gauge.TicketReleaseMarks;
import stack.moaticket.application.component.gauge.TicketReleaseRedisGaugeManager;
import stack.moaticket.application.component.handler.TicketReleaseHandler;
import stack.moaticket.application.component.producer.TicketReleaseProducer;
import stack.moaticket.application.model.*;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.system.redis.component.ops.RedisClient;
import stack.moaticket.system.redis.model.StreamMessage;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class TicketReleaseConsumerRunner {
    private final RedisClient redis;
    private final TicketReleaseHandler handler;
    private final TicketReleaseProducer producer;
    private final TicketReleaseRedisGaugeManager manager;
    private final ThreadPoolTaskExecutor executor;
    private final AlarmConsumerProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<Future<?>> workers = new CopyOnWriteArrayList<>();

    private final TicketReleaseConsumerKey consumerKey = new TicketReleaseConsumerKey();
    private final TicketReleaseLockValue lockValue = new TicketReleaseLockValue(true);
    private final TicketReleaseDoneValue doneValue = new TicketReleaseDoneValue(true);

    private static final String GROUP = "alarm-group";
    private static final String CONSUMER_PREFIX = "alarm-consumer-";
    private static final String PEL_PREFIX = "alarm-pel-";

    public TicketReleaseConsumerRunner(
            RedisClient redis,
            TicketReleaseHandler handler,
            TicketReleaseProducer producer,
            TicketReleaseRedisGaugeManager manager,
            @Qualifier("ticketReleaseRedisConsumerExecutor") ThreadPoolTaskExecutor executor,
            AlarmConsumerProperties properties) {
        this.redis = redis;
        this.handler = handler;
        this.producer = producer;
        this.manager = manager;
        this.executor = executor;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if(!running.compareAndSet(false, true)) return;

        redis.inner().stream().createGroupIfAbsent(
                new TicketReleaseConsumerKey(),
                GROUP);

        for(int i=1; i<=properties.ticketRelease().consumerThread(); i++) {
            String consumerName = CONSUMER_PREFIX + i;
            workers.add(executor.submit(() ->consumeLoop(consumerName)));
            log.info("TicketReleaseConsumer: {} is running", consumerName);
        }
        for(int i=1; i<=properties.ticketRelease().pelThread(); i++) {
            String consumerName = PEL_PREFIX + i;
            workers.add(executor.submit(() -> pelLoop(consumerName)));
            log.info("TicketReleaseConsumer: {} is running", consumerName);
        }
    }

    private void consumeLoop(String consumerName) {
        while(running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                StreamMessage<TicketReleaseConsumerValue> message = getConsumerMessage(consumerName);
                if(message == null) continue;
                manager.recordConsumer(marks -> processLoop(message, marks, false));
            } catch (Exception e) {
                log.error("TicketReleaseConsumerRunner: Alarm loop error. consumer={}", consumerName, e);
                backoff();
            }
        }
    }

    private void pelLoop(String consumerName) {
        Duration lockMillis = Duration.ofMillis(properties.ticketRelease().lockMillis());

        while(running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                StreamMessage<TicketReleaseConsumerValue> message = getPelMessage(consumerName, lockMillis);
                if(message == null) {
                    backoff();
                    continue;
                }
                manager.recordPel(marks -> processLoop(message, marks, true));
            } catch (Exception e) {
                log.error("TicketReleaseConsumerRunner: Alarm loop error. consumer={}", consumerName, e);
                backoff();
            }
        }
    }

    private void processLoop(
            StreamMessage<TicketReleaseConsumerValue> message,
            TicketReleaseMarks marks,
            boolean pel) {
        if (message.expiresAt() < System.currentTimeMillis()) {
            ack(message.id(), marks);
            return;
        }

        boolean done = redis.inner().basic().isExist(new TicketReleaseDoneKey(message.payload().id()));
        if (done) {
            ack(message.id(), marks);
            return;
        }

        TicketReleaseLockKey lockKey = new TicketReleaseLockKey(
                message.payload().id(),
                Duration.ofMillis(properties.ticketRelease().lockMillis()));
        boolean lock = redis.inner().basic().setIfAbsent(lockKey, lockValue);
        if (!lock) return;
        marks.markLocked();

        try {
            String runId = message.payload().refKey();
            TicketReleaseRunKey runKey = TicketReleaseRunKey.create(runId, false);
            TicketReleaseRunValue runValue = redis.inner().basic().get(runKey);
            if (runValue == null) return;

            List<Long> ticketIdList = runValue.ticketIdList();
            Map<Long, TicketMetaDto> meta = runValue.metadata();
            Long cursor = message.payload().cursor();

            Long nextCursor = handler.handle(ticketIdList, meta, cursor, properties.ticketRelease().limit());

            setDone(message.payload().id(), marks);
            ack(message.id(), marks);

            if (nextCursor != null) {
                TicketReleaseConsumerValue payload = new TicketReleaseConsumerValue(
                        TicketReleaseConsumerValue.createId(),
                        runId,
                        nextCursor);

                producer.publishContinue(payload, TicketReleaseConsumerValue.createExpiresAtMillis());
            }
        } catch (RejectedExecutionException e) {
            if(pel) {
                drop(message.id(), marks);
            } else {
                throw e;
            }
        } finally {
            unlock(lockKey, marks);
        }
    }

    private void backoff() {
        long backoff = properties.ticketRelease().backoffMillis();

        try { Thread.sleep(backoff); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @PreDestroy
    public void stop() {
        if(!running.compareAndSet(true, false)) return;

        for(Future<?> f : workers) {
            f.cancel(true);
        }
        workers.clear();
    }

    private StreamMessage<TicketReleaseConsumerValue> getConsumerMessage(String consumerName) {
        return redis.inner().stream().xRead(
                consumerKey,
                GROUP,
                consumerName,
                Duration.ofSeconds(2)
        );
    }

    private StreamMessage<TicketReleaseConsumerValue> getPelMessage(String consumerName, Duration lockMillis) {
        return redis.inner().stream().xAutoClaim(
                consumerKey,
                GROUP,
                consumerName,
                lockMillis
        );
    }

    private void ack(RecordId id, TicketReleaseMarks marks) {
        redis.inner().stream().xAck(
                consumerKey,
                GROUP,
                id);
        marks.markAcked();
    }

    public void drop(RecordId id, TicketReleaseMarks marks) {
        ack(id, marks);
        marks.markDropped();
    }

    private void unlock(TicketReleaseLockKey lockKey, TicketReleaseMarks marks) {
        redis.inner().basic().remove(lockKey);
        marks.markUnlocked();
    }

    private void setDone(String id, TicketReleaseMarks marks) {
        redis.inner().basic().set(
                new TicketReleaseDoneKey(id),
                doneValue
        );
        marks.markDone();
    }
}
