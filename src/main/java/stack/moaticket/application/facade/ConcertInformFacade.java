package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.session_start_alarm.service.SessionStartAlarmService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertInformFacade {
    private final SessionStartAlarmService sessionStartAlarmService;
    
    public List<Long> extractAlarms(LocalDateTime now, Long batchSize) {
        return sessionStartAlarmService.getPendingSessionAlarmIdList(now, batchSize);
    }

    @Transactional
    public void passAndProcess(LocalDateTime now, List<Long> alarmList) {
        sessionStartAlarmService.updatePendingToPassed(now, alarmList);
        sessionStartAlarmService.updatePendingToProcessed(now, alarmList);
    }

    public List<SessionStartAlarmMetaDto> getCurrentProcessedCandidates(List<Long> alarmIdList) {
        return sessionStartAlarmService.getProcessedSessionAlarmList(alarmIdList);
    }
}
