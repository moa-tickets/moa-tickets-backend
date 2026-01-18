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
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ConcertStartInformJobTest {
    @Mock AlarmService alarmService;
    @Mock ConcertInformFacade concertInformFacade;

    @InjectMocks ConcertStartInformJob job;

    @Test
    @DisplayName("후보 존재 시 1회 처리되고, 이후 후보가 없을 경우 종료한다.")
    void test() {
        // given
        List<SessionStartAlarm> candidateList = List.of(mock(SessionStartAlarm.class));
        List<SessionStartAlarm> claimed = List.of(mock(SessionStartAlarm.class));

        List<SessionStartAlarm> succeeded = List.of(mock(SessionStartAlarm.class));
        List<SessionStartAlarm> disconnected = List.of(mock(SessionStartAlarm.class));
        List<List<SessionStartAlarm>> results = List.of(succeeded, disconnected);

        given(concertInformFacade.extractCandidates(any(LocalDateTime.class), eq(200L)))
                .willReturn(candidateList)
                .willReturn(List.of());

        given(concertInformFacade.getCurrentClaimedCandidates(any(LocalDateTime.class)))
                .willReturn(claimed);

        given(alarmService.sendConcertStartInform(claimed))
                .willReturn(results);

        // when
        job.runEpoch();

        // then
        InOrder inOrder = inOrder(concertInformFacade, alarmService);

        inOrder.verify(concertInformFacade).cleanup(any(LocalDateTime.class));
        inOrder.verify(concertInformFacade).extractCandidates(any(LocalDateTime.class), eq(200L));
        inOrder.verify(concertInformFacade).skipAndClaim(eq(candidateList), any(LocalDateTime.class));
        inOrder.verify(concertInformFacade).getCurrentClaimedCandidates(any(LocalDateTime.class));
        inOrder.verify(alarmService).sendConcertStartInform(eq(claimed));
        inOrder.verify(concertInformFacade).applyResults(eq(succeeded), eq(disconnected));

        then(concertInformFacade).should(times(2))
                .extractCandidates(any(LocalDateTime.class), eq(200L));
    }
}
