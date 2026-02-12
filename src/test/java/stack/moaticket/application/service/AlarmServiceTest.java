package stack.moaticket.application.service;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.system.alarm.core.util.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.service.AlarmSendService;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.ticket.service.TicketService;
import stack.moaticket.domain.ticket_alarm.service.TicketAlarmService;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {
    @Spy private Validator validator;

    @Mock private AlarmMessageFactory alarmMessageFactory;
    @Mock private AlarmSendService alarmSendService;

    @Mock private SseSubscribeService sseSubscribeService;

    @Mock private MemberService memberService;
    @Mock private TicketService ticketService;
    @Mock private TicketAlarmService ticketAlarmService;

    @InjectMocks private AlarmService alarmService;

    // Method: subscribe()
    @Test
    @DisplayName("Subscribe를 성공하면 Emitter를 반환한다.")
    void subscribeSuccess() {
        // given
        Long mid = 1L;

        Member member = mock(Member.class);
        given(memberService.findById(mid)).willReturn(member);
        given(member.getState()).willReturn(MemberState.ACTIVE);

        SseEmitter emitter = mock(SseEmitter.class);
        given(sseSubscribeService.subscribe(mid)).willReturn(emitter);

        // when
        SseEmitter ret = alarmService.subscribe(mid);

        // then
        then(sseSubscribeService).should().subscribe(mid);
        BDDAssertions.then(ret).isSameAs(emitter);
    }

    @Test
    @DisplayName("유효하지 않은 회원이면 MEMBER_NOT_FOUND 예외가 발생하고 Subscribe를 호출하지 않는다.")
    void subscribeFailedMemberNotFound() {
        // given
        Long mid = 1L;
        Member member = mock(Member.class);
        given(memberService.findById(mid)).willReturn(null);

        // when & then
        BDDAssertions.thenThrownBy(() -> alarmService.subscribe(mid))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MEMBER_NOT_FOUND);

        then(sseSubscribeService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("ACTIVE 상태인 회원이 아니면 UNAUTHORIZED 예외가 발생하고 Subscribe를 호출하지 않는다.")
    void subscribeFailedUnauthorized() {
        // given
        Long mid = 1L;
        Member member = mock(Member.class);
        given(memberService.findById(mid)).willReturn(member);
        given(member.getState()).willReturn(MemberState.BLOCKED);

        // when & then
        BDDAssertions.thenThrownBy(() -> alarmService.subscribe(mid))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.UNAUTHORIZED);

        then(sseSubscribeService).shouldHaveNoMoreInteractions();
    }

    // Method: subscribeTicketReleaseAlarm()
    @Test
    @DisplayName("티켓 알림을 등록한다.")
    void subscribeTicketAlarmSuccess() {
        // given
        Long mid = 1L;
        Long tid = 10L;

        Member member = mock(Member.class);
        given(memberService.findById(mid)).willReturn(member);
        given(member.getState()).willReturn(MemberState.ACTIVE);

        Ticket ticket = mock(Ticket.class);
        given(ticketService.get(tid)).willReturn(ticket);

        // when
        alarmService.subscribeTicketReleaseAlarm(mid, tid);

        // then
        then(ticketAlarmService).should().createAndSave(member, ticket);

        then(memberService).should().findById(mid);
        then(ticketService).should().get(tid);

        then(ticketAlarmService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 회원이면 MEMBER_NOT_FOUND 예외가 발생하고 SubscribeTicketReleaseAlarm을 호출하지 않는다.")
    void subscribeTicketAlarmFailedMemberNotFound() {
        // given
        Long mid = 1L;
        Long tid = 10L;
        given(memberService.findById(mid)).willReturn(null);

        // when & then
        BDDAssertions.thenThrownBy(() -> alarmService.subscribeTicketReleaseAlarm(mid, tid))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MEMBER_NOT_FOUND);

        then(ticketService).shouldHaveNoMoreInteractions();
        then(ticketAlarmService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("ACTIVE 상태인 회원이 아니면 UNAUTHORIZED 예외가 발생하고 SubscribeTicketReleaseAlarm을 호출하지 않는다.")
    void subscribeTicketAlarmFailedUnauthorized() {
        // given
        Long mid = 1L;
        Long tid = 10L;
        Member member = mock(Member.class);
        given(memberService.findById(mid)).willReturn(member);
        given(member.getState()).willReturn(MemberState.BLOCKED);

        // when & then
        BDDAssertions.thenThrownBy(() -> alarmService.subscribeTicketReleaseAlarm(mid, tid))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.UNAUTHORIZED);

        then(ticketService).shouldHaveNoMoreInteractions();
        then(ticketAlarmService).shouldHaveNoMoreInteractions();
    }

    // Method: unsubscribeTicketReleaseAlarm()
    @Test
    @DisplayName("티켓 알림 등록을 해제한다.")
    void unsubscribeTicketAlarmSuccess() {
        // given
        Long mid = 1L;
        Long tid = 10L;

        Member member = mock(Member.class);
        given(memberService.findById(mid)).willReturn(member);
        given(member.getState()).willReturn(MemberState.ACTIVE);

        Ticket ticket = mock(Ticket.class);
        given(ticketService.get(tid)).willReturn(ticket);

        // when
        alarmService.unsubscribeTicketReleaseAlarm(mid, tid);

        // then
        then(memberService).should().findById(mid);
        then(ticketService).should().get(tid);
        then(ticketAlarmService).should().delete(member, ticket);

        then(memberService).shouldHaveNoMoreInteractions();
        then(ticketService).shouldHaveNoMoreInteractions();
        then(ticketAlarmService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 회원이면 MEMBER_NOT_FOUND 예외가 발생하고 UnsubscribeTicketReleaseAlarm을 호출하지 않는다.")
    void unsubscribeTicketAlarmFailedMemberNotFound() {
        // given
        Long mid = 1L;
        Long tid = 10L;
        given(memberService.findById(mid)).willReturn(null);

        // when & then
        BDDAssertions.thenThrownBy(() -> alarmService.unsubscribeTicketReleaseAlarm(mid, tid))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MEMBER_NOT_FOUND);

        then(ticketService).shouldHaveNoMoreInteractions();
        then(ticketAlarmService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("ACTIVE 상태인 회원이 아니면 UNAUTHORIZED 예외가 발생하고 UnsubscribeTicketReleaseAlarm을 호출하지 않는다.")
    void unsubscribeTicketAlarmFailedUnauthorized() {
        // given
        Long mid = 1L;
        Long tid = 10L;
        Member member = mock(Member.class);
        given(memberService.findById(mid)).willReturn(member);
        given(member.getState()).willReturn(MemberState.BLOCKED);

        // when & then
        BDDAssertions.thenThrownBy(() -> alarmService.unsubscribeTicketReleaseAlarm(mid, tid))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.UNAUTHORIZED);

        then(ticketService).shouldHaveNoMoreInteractions();
        then(ticketAlarmService).shouldHaveNoMoreInteractions();
    }

    // Method: sendConcertStartInform()
    @Test
    @DisplayName("콘서트 시작 알림 메타데이터를 순회하며 메시지를 생성하고, 각 사용자에게 전달한다.")
    void sendConcertStartInformSendsForEachAlarm() {
        // given
        SessionStartAlarmMetaDto a1 = mock(SessionStartAlarmMetaDto.class);
        SessionStartAlarmMetaDto a2 = mock(SessionStartAlarmMetaDto.class);

        given(a1.memberId()).willReturn(1L);
        given(a2.memberId()).willReturn(2L);

        given(a1.type()).willReturn(SessionStartAlarmType.LEFT_10);
        given(a2.type()).willReturn(SessionStartAlarmType.ON_HOUR);

        // when
        alarmService.sendConcertStartInform(List.of(a1, a2));

        // then
        InOrder inOrder = inOrder(alarmSendService);

        inOrder.verify(alarmSendService).sendAll(eq(1L), argThat(msg ->
                "SS_LEFT_10".equals(msg.key()) && msg.payload() == a1));
        inOrder.verify(alarmSendService).sendAll(eq(2L), argThat(msg ->
                "SS_ON_HOUR".equals(msg.key()) && msg.payload() == a2));

        then(alarmSendService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("콘서트 시작 알림 메타데이터가 비어있으면 아무 것도 수행하지 않는다.")
    void sendConcertStartInformIfEmptyListDoesNothing() {
        // when
        alarmService.sendConcertStartInform(List.of());

        // then
        then(alarmMessageFactory).shouldHaveNoMoreInteractions();
        then(alarmSendService).shouldHaveNoMoreInteractions();
    }

    // Method: sendTicketReleaseInfrom()
    @Test
    @DisplayName("ReceiverMap의 각 멤버에 대해 유효한 티켓 메타데이터가 존재하면 메시지를 생성하고 전송한다.")
    void sendTicketReleaseInformSendWhenMetaExists() {
        // given
        Long m1 = 1L;
        Long m2 = 2L;

        Long t1 = 10L;
        Long t2 = 20L;
        Long t3 = 30L;

        TicketMetaDto meta1 = mock(TicketMetaDto.class);
        TicketMetaDto meta2 = mock(TicketMetaDto.class);
        TicketMetaDto meta3 = mock(TicketMetaDto.class);

        Map<Long, List<Long>> receiverMap = new HashMap<>();
        receiverMap.put(m1, List.of(t1, t2));
        receiverMap.put(m2, List.of(t3));

        Map<Long, TicketMetaDto> ticketMetadata = new HashMap<>();
        ticketMetadata.put(t1, meta1);
        ticketMetadata.put(t2, meta2);
        ticketMetadata.put(t3, meta3);

        // when
        alarmService.sendTicketReleaseInform(receiverMap, ticketMetadata);

        // then
        ArgumentCaptor<AlarmMessage> captor = ArgumentCaptor.forClass(AlarmMessage.class);
        InOrder inOrder = inOrder(alarmSendService);

        inOrder.verify(alarmSendService).sendAll(eq(m1), captor.capture());
        inOrder.verify(alarmSendService).sendAll(eq(m2), captor.capture());

        List<AlarmMessage> messages = captor.getAllValues();
        assertThat(messages).hasSize(2);

        then(alarmSendService).should().sendAll(eq(m1), argThat(msg ->
                "TR_BULK".equals(msg.key())
                && msg.payload() instanceof List<?> list
                && list.size() == 2
                && list.contains(meta1)
                && list.contains(meta2)
        ));

        then(alarmSendService).should().sendAll(eq(m2), argThat(msg ->
                "TR_SINGLE".equals(msg.key())
                        && msg.payload() instanceof List<?> list
                        && list.size() == 1
                        && list.contains(meta3)
        ));

        then(alarmSendService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("TicketIdList가 비어있으면 스킵한다.")
    void sendTicketReleaseInformSkipsWhenTicketIdListIsEmpty() {
        // given
        Map<Long, List<Long>> receiverMap = new HashMap<>();
        receiverMap.put(1L, List.of());

        // when
        alarmService.sendTicketReleaseInform(receiverMap, Map.of());

        // then
        then(alarmMessageFactory).shouldHaveNoMoreInteractions();
        then(alarmSendService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("TicketMetadata에서 유효한 metadata가 하나도 없을 시 스킵한다.")
    void sendTicketReleaseInformSkipsWhenTicketMetadataIsNull() {
        // given
        Long mid = 1L;
        Map<Long, List<Long>> receiverMap = Map.of(mid, List.of(10L, 20L));
        Map<Long, TicketMetaDto> ticketMetadata = Map.of();

        // when
        alarmService.sendTicketReleaseInform(receiverMap, ticketMetadata);

        // then
        then(alarmMessageFactory).shouldHaveNoMoreInteractions();
        then(alarmSendService).shouldHaveNoMoreInteractions();
    }
}
