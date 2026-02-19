package stack.moaticket.application.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.component.scheduler.JobSchedulerProperties;
import stack.moaticket.application.facade.ConcertInformFacade;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ConcertStartInformJob {
    private final AlarmService alarmService;
    private final ConcertInformFacade concertInformFacade;
    private final ThreadPoolTaskExecutor executor;
    private final JobSchedulerProperties properties;

    public ConcertStartInformJob(
            AlarmService alarmService,
            ConcertInformFacade facade,
            @Qualifier("sessionStartExecutor") ThreadPoolTaskExecutor executor,
            JobSchedulerProperties properties) {
        this.alarmService = alarmService;
        this.concertInformFacade = facade;
        this.executor = executor;
        this.properties = properties;
    }

    public void runEpoch() {
        executor.execute(() -> {
            Long batchSize = properties.sessionStart().batchSize();
            LocalDateTime now = LocalDateTime.now();

            List<Long> idList = concertInformFacade.passAndProcess(now, batchSize);
            List<SessionStartAlarmMetaDto> alarmMetaData = concertInformFacade.getCurrentProcessedCandidates(idList);
            if(alarmMetaData.isEmpty()) return;

            alarmService.sendConcertStartInform(alarmMetaData);
        });
    }
}
