package stack.moaticket.application.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.application.facade.HoldCleanedInformFacade;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket_alarm.dto.TicketAlarmDto;
import stack.moaticket.domain.ticket_alarm.service.TicketAlarmService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketReleaseInformJobTest {
    @Mock AlarmService alarmService;
    @Mock TicketAlarmService ticketAlarmService;
    @Mock HoldCleanedInformFacade holdCleanedInformFacade;

    @InjectMocks TicketReleaseInformJob job;

    @Test
    @DisplayName("후보가 없으면 즉시 종료한다.")
    void stopImmediatelyWhenNoCandidates() {
        // given
        given(holdCleanedInformFacade.extractCandidates(any(LocalDateTime.class), eq(200L)))
                .willReturn(List.of());

        // when
        job.runEpoch();

        // then
        then(holdCleanedInformFacade).should(times(1))
                .extractCandidates(any(LocalDateTime.class), eq(200L));
        then(holdCleanedInformFacade).should(never())
                .release(any(), anyList());
        then(holdCleanedInformFacade).should(never())
                .getChanged(anyList());
        then(ticketAlarmService).shouldHaveNoMoreInteractions();
        then(alarmService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("후보가 존재하면 Release 후 페이징 전송을 수행하고, DtoList가 비면 종료한다.")
    @SuppressWarnings(value = "unchecked")
    void processOneCycle() {
        // given
        List<Long> tids = List.of(10L, 20L, 30L);

        TicketMetaDto m10 = mock(TicketMetaDto.class);
        TicketMetaDto m20 = mock(TicketMetaDto.class);
        TicketMetaDto m30 = mock(TicketMetaDto.class);
        given(m10.ticketId()).willReturn(10L);
        given(m20.ticketId()).willReturn(20L);
        given(m30.ticketId()).willReturn(30L);

        given(holdCleanedInformFacade.extractCandidates(any(LocalDateTime.class), eq(200L)))
                .willReturn(tids, List.of());
        given(holdCleanedInformFacade.getChanged(eq(tids)))
                .willReturn(List.of(m10, m20, m30));

        TicketAlarmDto p1a = mock(TicketAlarmDto.class);
        TicketAlarmDto p1b = mock(TicketAlarmDto.class);
        given(p1b.id()).willReturn(200L);
        given(p1a.memberId()).willReturn(1L);
        given(p1a.ticketId()).willReturn(10L);
        given(p1b.memberId()).willReturn(1L);
        given(p1b.ticketId()).willReturn(20L);

        TicketAlarmDto p2a = mock(TicketAlarmDto.class);
        given(p2a.id()).willReturn(300L);
        given(p2a.memberId()).willReturn(2L);
        given(p2a.ticketId()).willReturn(30L);

        AtomicInteger call = new AtomicInteger(0);
        doAnswer(inv -> {
            List<Long> argIds = inv.getArgument(0);
            Long cursor = inv.getArgument(1);
            int limit = inv.<Integer>getArgument(2);

            assertThat(argIds).isEqualTo(tids);
            assertThat(limit).isEqualTo(200);

            int n = call.getAndIncrement();
            if(n == 0) {
                assertThat(cursor).isNull();
                return List.of(p1a, p1b);
            }
            if(n == 1) {
                assertThat(cursor).isNotNull();
                return List.of(p2a);
            }
            assertThat(cursor).isEqualTo(300L);
            return List.of();
        }).when(ticketAlarmService).getDtoList(eq(tids), any(), anyInt());

        // when
        job.runEpoch();

        // then
        InOrder inOrder = inOrder(holdCleanedInformFacade, ticketAlarmService, alarmService);

        /* Cycle 1 */
        inOrder.verify(holdCleanedInformFacade)
                .extractCandidates(any(LocalDateTime.class), eq(200L));
        inOrder.verify(holdCleanedInformFacade)
                .release(any(LocalDateTime.class), eq(tids));
        inOrder.verify(holdCleanedInformFacade)
                .getChanged(eq(tids));
        /* Page 1 */
        inOrder.verify(ticketAlarmService)
                .getDtoList(eq(tids), isNull(), eq(200));
        inOrder.verify(alarmService)
                .sendTicketReleaseInform(anyMap(), anyMap());
        /* Page 2 */
        inOrder.verify(ticketAlarmService)
                .getDtoList(eq(tids), eq(200L), eq(200));
        inOrder.verify(alarmService)
                .sendTicketReleaseInform(anyMap(), anyMap());
        /* Page 3 - EMPTY */
        inOrder.verify(ticketAlarmService)
                .getDtoList(eq(tids), eq(300L), eq(200));

        /* Cycle 2 */
        inOrder.verify(holdCleanedInformFacade)
                .extractCandidates(any(LocalDateTime.class), eq(200L));


        then(holdCleanedInformFacade).should(times(2))
                .extractCandidates(any(LocalDateTime.class), eq(200L));
        then(holdCleanedInformFacade).should(times(1))
                .release(any(LocalDateTime.class), eq(tids));
        then(holdCleanedInformFacade).should(times(1))
                .getChanged(eq(tids));

        then(ticketAlarmService).should(times(3))
                .getDtoList(eq(tids), any(), eq(200));
        then(alarmService).should(times(2))
                .sendTicketReleaseInform(anyMap(), anyMap());

        ArgumentCaptor<Map<Long, List<Long>>> receiverCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<Long, TicketMetaDto>> metaCaptor = ArgumentCaptor.forClass(Map.class);

        then(alarmService).should(times(2)).
                sendTicketReleaseInform(receiverCaptor.capture(), metaCaptor.capture());

        for(Map<Long, TicketMetaDto> meta : metaCaptor.getAllValues()) {
            assertThat(meta).containsKeys(10L, 20L, 30L);
        }

        // Page 1
        Map<Long, List<Long>> first = receiverCaptor.getAllValues().get(0);
        assertThat(first).containsKey(1L);
        assertThat(first.get(1L)).containsExactlyInAnyOrder(10L, 20L);

        // Page 2
        Map<Long, List<Long>> second = receiverCaptor.getAllValues().get(1);
        assertThat(second).containsKey(2L);
        assertThat(second.get(2L)).containsExactlyInAnyOrder(30L);
    }
}
