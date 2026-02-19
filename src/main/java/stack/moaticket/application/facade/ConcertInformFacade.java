package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.component.gauge.SessionStartGaugeManager;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.session_start_alarm.service.SessionStartAlarmService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertInformFacade {
    private final SessionStartAlarmService sessionStartAlarmService;
    private final SessionStartGaugeManager manager;

    @Transactional
    public List<Long> passAndProcess(LocalDateTime now, Long batchSize) {
        List<Long> idList = sessionStartAlarmService.getPendingSessionAlarmIdList(now, batchSize);
        if(idList.isEmpty()) return List.of();

        manager.recordDatabase(() -> sessionStartAlarmService.updateAlarmIdList(idList, now));
        return idList;
    }

    public List<SessionStartAlarmMetaDto> getCurrentProcessedCandidates(List<Long> alarmIdList) {
        return sessionStartAlarmService.getProcessedSessionAlarmList(alarmIdList);
    }
}
