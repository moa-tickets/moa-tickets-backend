package stack.moaticket.application.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.application.facade.ConcertInformFacade;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ConcertStartInformJobTest {
    @Mock private AlarmService alarmService;
    @Mock private ConcertInformFacade concertInformFacade;

    @InjectMocks private ConcertStartInformJob job;

    @Test
    @DisplayName("후보 존재 시 1회 처리되고, 이후 후보가 없을 경우 종료한다.")
    void processOneCycle() {
        // given
        List<Long> alarmList = List.of(mock(Long.class));
        List<SessionStartAlarmMetaDto> metaList = List.of(mock(SessionStartAlarmMetaDto.class));

        given(concertInformFacade.extractAlarms(any(LocalDateTime.class), eq(200L)))
                .willReturn(alarmList)
                .willReturn(List.of());
        given(concertInformFacade.getCurrentProcessedCandidates(alarmList))
                .willReturn(metaList);

        // when
        job.runEpoch();

        // then
        InOrder inOrder = inOrder(concertInformFacade, alarmService);

        inOrder.verify(concertInformFacade).extractAlarms(any(LocalDateTime.class), eq(200L));
        inOrder.verify(concertInformFacade).passAndProcess(any(LocalDateTime.class), eq(alarmList));
        inOrder.verify(concertInformFacade).getCurrentProcessedCandidates(alarmList);
        inOrder.verify(alarmService).sendConcertStartInform(metaList);

        then(concertInformFacade).should(times(2))
                .extractAlarms(any(LocalDateTime.class), eq(200L));
    }
}
