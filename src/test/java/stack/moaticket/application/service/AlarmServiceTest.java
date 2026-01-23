package stack.moaticket.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.application.component.factory.AlarmMessageFactory;
import stack.moaticket.application.port.AlarmMessage;
import stack.moaticket.application.port.AlarmSender;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.ticket.service.TicketService;
import stack.moaticket.domain.ticket_alarm.service.TicketAlarmService;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.sse.service.SseSubscribeService;

import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {
    @Mock private Validator validator;

    @Mock private AlarmMessageFactory alarmMessageFactory;
    @Mock private AlarmSender alarmSender;

    @Mock private SseSubscribeService sseSubscribeService;

    @Mock private MemberService memberService;
    @Mock private TicketService ticketService;
    @Mock private TicketAlarmService ticketAlarmService;

    @InjectMocks private AlarmService alarmService;

    @Test
    @DisplayName("콘서트 시작 알림 메타데이터를 순회하며 메시지를 생성하고, 각 사용자에게 전달한다.")
    void sendConcertStartInformSendsForEachAlarm() {
        // given
        SessionStartAlarmMetaDto a1 = mock(SessionStartAlarmMetaDto.class);
        SessionStartAlarmMetaDto a2 = mock(SessionStartAlarmMetaDto.class);

        given(a1.memberId()).willReturn(1L);
        given(a2.memberId()).willReturn(2L);

        AlarmMessage m1 = new AlarmMessage("SS_LEFT_10", a1);
        AlarmMessage m2 = new AlarmMessage("SS_ON_HOUR", a2);

        given(alarmMessageFactory.sessionStart(a1)).willReturn(m1);
        given(alarmMessageFactory.sessionStart(a2)).willReturn(m2);

        // when
        alarmService.sendConcertStartInform(List.of(a1, a2));

        // then
        InOrder inOrder = inOrder(alarmMessageFactory, alarmSender);

        inOrder.verify(alarmMessageFactory).sessionStart(a1);
        inOrder.verify(alarmSender).sendAll(1L, m1);

        inOrder.verify(alarmMessageFactory).sessionStart(a2);
        inOrder.verify(alarmSender).sendAll(2L, m2);

        then(alarmMessageFactory).shouldHaveNoMoreInteractions();
        then(alarmSender).shouldHaveNoMoreInteractions();
    }
}
