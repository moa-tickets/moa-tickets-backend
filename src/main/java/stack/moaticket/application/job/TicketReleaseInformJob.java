package stack.moaticket.application.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.component.producer.TicketReleaseProducer;
import stack.moaticket.application.component.scheduler.JobSchedulerProperties;
import stack.moaticket.application.facade.HoldCleanedInformFacade;
import stack.moaticket.application.model.*;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.system.redis.model.RedisKey;
import stack.moaticket.system.redis.model.RedisValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TicketReleaseInformJob {
    private final TicketReleaseProducer producer;
    private final HoldCleanedInformFacade holdCleanedInformFacade;
    private final ThreadPoolTaskExecutor executor;
    private final JobSchedulerProperties properties;

    public TicketReleaseInformJob(
            TicketReleaseProducer producer,
            HoldCleanedInformFacade facade,
            @Qualifier("ticketReleaseExecutor") ThreadPoolTaskExecutor executor,
            JobSchedulerProperties properties) {
        this.producer = producer;
        this.holdCleanedInformFacade = facade;
        this.executor = executor;
        this.properties = properties;
    }

    public void runEpoch() {
        Long batchSize = properties.ticketRelease().batchSize();

        executor.execute(() -> {
            LocalDateTime now = LocalDateTime.now();

            List<Long> ticketIdList = holdCleanedInformFacade.release(now, batchSize);
            if(ticketIdList.isEmpty()) return;

            Map<Long, TicketMetaDto> ticketMetadata = holdCleanedInformFacade.getChanged(ticketIdList);

            TicketReleaseRunKey runKey = TicketReleaseRunKey.create();
            TicketReleaseRunValue runValue = new TicketReleaseRunValue(ticketIdList, ticketMetadata);

            String releaseValueId = TicketReleaseConsumerValue.createId();
            TicketReleaseConsumerValue value = new TicketReleaseConsumerValue(
                    releaseValueId,
                    runKey.get(),
                    null);

            List<RedisKey<? extends RedisValue>> keys = new ArrayList<>();
            keys.add(runKey);
            keys.add(new TicketReleaseConsumerKey());

            producer.publishFirst(
                    keys,
                    runValue,
                    runKey.ttl(),
                    value,
                    TicketReleaseConsumerValue.createExpiresAtMillis());
        });
    }
}
