package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.domain.session_start_alarm.service.SessionStartAlarmService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertInformFacade {
    private final SessionStartAlarmService sessionStartAlarmService;

    @Transactional
    public void cleanup(LocalDateTime now) {
        sessionStartAlarmService.cleanupTerminatedClaimedAlarm(now);
    }

    public List<SessionStartAlarm> extractCandidates(LocalDateTime now, Long batchSize) {
        return sessionStartAlarmService.getPendingSessionAlarmList(batchSize, now);
    }

    @Transactional
    public void skipAndClaim(List<SessionStartAlarm> candidateList, LocalDateTime now) {
        sessionStartAlarmService.updatePendingToSkipped(candidateList, now);
        sessionStartAlarmService.updatePendingToClaimed(candidateList, now);
    }

    public List<SessionStartAlarm> getCurrentClaimedCandidates(LocalDateTime now) {
        return sessionStartAlarmService.getClaimedSessionAlarmList(now);
    }

    @Transactional
    public void applyResults(List<SessionStartAlarm> succeededList, List<SessionStartAlarm> disconnectedList) {
        if(!succeededList.isEmpty()) sessionStartAlarmService.updateClaimedToSent(succeededList);
        if(!disconnectedList.isEmpty()) sessionStartAlarmService.updateClaimedToPendingOrDisconnected(disconnectedList);
    }
}
