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
    @SuppressWarnings(value = "unchecked")
    void processOneCycle() {
        // given
        List<Long> alarmList = List.of(1L);
        List<SessionStartAlarmMetaDto> metaList = List.of(mock(SessionStartAlarmMetaDto.class));

        given(concertInformFacade.extractAlarms(any(LocalDateTime.class), eq(200L)))
                .willReturn(alarmList, List.of());
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

        inOrder.verify(concertInformFacade).extractAlarms(any(LocalDateTime.class), eq(200L));
        inOrder.verifyNoMoreInteractions();

        then(alarmService).should(times(1))
                .sendConcertStartInform(metaList);
        then(concertInformFacade).should(times(2))
                .extractAlarms(any(LocalDateTime.class), eq(200L));
        then(alarmService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("알림 후보가 없으면 즉시 종료한다.")
    void stopImmediatelyWhenNoAlarms() {
        // given
        given(concertInformFacade.extractAlarms(any(LocalDateTime.class), eq(200L)))
                .willReturn(List.of());

        // when
        job.runEpoch();

        // then
        then(concertInformFacade).should(times(1))
                .extractAlarms(any(LocalDateTime.class), eq(200L));
        then(concertInformFacade).should(never())
                .passAndProcess(any(), anyList());
        then(concertInformFacade).should(never())
                .getCurrentProcessedCandidates(anyList());
        then(alarmService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("후보가 더 존재하더라도, 1회의 Job에 5사이클만 처리한다.")
    void shouldStopAfterRetryLimit() {
        // given
        List<Long> alarmList = List.of(1L);
        List<SessionStartAlarmMetaDto> metaList = List.of(mock(SessionStartAlarmMetaDto.class));

        given(concertInformFacade.extractAlarms(any(LocalDateTime.class), eq(200L)))
                .willReturn(alarmList);
        given(concertInformFacade.getCurrentProcessedCandidates(eq(alarmList)))
                .willReturn(metaList);

        // when
        job.runEpoch();

        // then
        then(concertInformFacade).should(times(5))
                .extractAlarms(any(LocalDateTime.class), eq(200L));
        then(concertInformFacade).should(times(5))
                .passAndProcess(any(LocalDateTime.class), eq(alarmList));
        then(concertInformFacade).should(times(5))
                .getCurrentProcessedCandidates(eq(alarmList));
        then(alarmService).should(times(5))
                .sendConcertStartInform(eq(metaList));
    }
}
