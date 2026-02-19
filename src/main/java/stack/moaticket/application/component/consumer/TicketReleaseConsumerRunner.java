package stack.moaticket.application.component.consumer;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class TicketReleaseConsumerRunner {
    private final RedisClient redis;
    private final TicketReleaseHandler handler;
    private final TicketReleaseProducer producer;
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
            @Qualifier("ticketReleaseRedisConsumerExecutor") ThreadPoolTaskExecutor executor,
            AlarmConsumerProperties properties) {
        this.redis = redis;
        this.handler = handler;
        this.producer = producer;
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
            workers.add(executor.submit(() -> consumeLoop(consumerName)));
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
                processLoop(message);
            } catch (Exception e) {
                log.error("TicketReleaseConsumerRunner: Alarm loop error. consumer={}", consumerName, e);
                backoff();
            }
        }
    }

    private void pelLoop(String consumerName) {
        while(running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                StreamMessage<TicketReleaseConsumerValue> message = getPelMessage(consumerName);
                if(message == null) {
                    backoff();
                    continue;
                }
                processLoop(message);
            } catch (Exception e) {
                log.error("TicketReleaseConsumerRunner: Alarm loop error. consumer={}", consumerName, e);
                backoff();
            }
        }
    }

    private void processLoop(StreamMessage<TicketReleaseConsumerValue> message) {
        if(message.expiresAt() < System.currentTimeMillis()) {
            ack(message.id());
            return;
        }

        boolean done = redis.inner().basic().isExist(new TicketReleaseDoneKey(message.payload().id()));
        if(done) {
            ack(message.id());
            return;
        }

        TicketReleaseLockKey lockKey = new TicketReleaseLockKey(message.payload().id());
        boolean lock = redis.inner().basic().setIfAbsent(lockKey, lockValue);
        if(!lock) return;

        try {
            String runId = message.payload().refKey();
            TicketReleaseRunKey runKey = TicketReleaseRunKey.create(runId, false);
            TicketReleaseRunValue runValue = redis.inner().basic().get(runKey);
            if(runValue == null) return;

            List<Long> ticketIdList = runValue.ticketIdList();
            Map<Long, TicketMetaDto> meta = runValue.metadata();
            Long cursor = message.payload().cursor();

            Long nextCursor = handler.handle(ticketIdList, meta, cursor, properties.ticketRelease().limit());

            setDone(message.payload().id());
            ack(message.id());

            if(nextCursor != null) {
                TicketReleaseConsumerValue payload = new TicketReleaseConsumerValue(
                        TicketReleaseConsumerValue.createId(),
                        runId,
                        nextCursor);

                producer.publishContinue(payload, TicketReleaseConsumerValue.createExpiresAtMillis());
            }
        } finally {
            unlock(lockKey);
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

    private StreamMessage<TicketReleaseConsumerValue> getPelMessage(String consumerName) {
        return redis.inner().stream().xAutoClaim(
                consumerKey,
                GROUP,
                consumerName
        );
    }

    private void ack(RecordId id) {
        redis.inner().stream().xAck(
                consumerKey,
                GROUP,
                id);
    }

    private void unlock(TicketReleaseLockKey lockKey) {
        redis.inner().basic().remove(lockKey);
    }

    private void setDone(String id) {
        redis.inner().basic().set(
                new TicketReleaseDoneKey(id),
                doneValue
        );
    }
}
