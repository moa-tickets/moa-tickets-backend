package stack.moaticket.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stack.moaticket.application.facade.ConcertInformFacade;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertStartInformJob {
    private final AlarmService alarmService;
    private final ConcertInformFacade concertInformFacade;

    private static final Long BATCH_SIZE = 200L;

    public void runEpoch() {
        int retry = 5;
        LocalDateTime now = LocalDateTime.now();

        while(retry-- > 0) {
            List<Long> alarmList = concertInformFacade.extractAlarms(now, BATCH_SIZE);
            if(alarmList.isEmpty()) break;

            concertInformFacade.passAndProcess(now, alarmList);
            List<SessionStartAlarmMetaDto> alarmMetaData = concertInformFacade.getCurrentProcessedCandidates(alarmList);
            if(alarmMetaData.isEmpty()) break;

            alarmService.sendConcertStartInform(alarmMetaData);
        }
    }
}
