package stack.moaticket.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stack.moaticket.application.facade.ConcertInformFacade;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;

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
        concertInformFacade.cleanup(now);

        while(retry-- > 0) {
            List<SessionStartAlarm> candidateList = concertInformFacade.extractCandidates(now, BATCH_SIZE);
            if(candidateList.isEmpty()) break;

            concertInformFacade.skipAndClaim(candidateList, now);
            candidateList = concertInformFacade.getCurrentClaimedCandidates(now);
            if(candidateList.isEmpty()) break;

            List<List<SessionStartAlarm>> sendResults = alarmService.sendConcertStartInform(candidateList);
            List<SessionStartAlarm> succeededList = sendResults.get(0);
            List<SessionStartAlarm> disconnectedList = sendResults.get(1);

            concertInformFacade.applyResults(succeededList, disconnectedList);
        }
    }
}
