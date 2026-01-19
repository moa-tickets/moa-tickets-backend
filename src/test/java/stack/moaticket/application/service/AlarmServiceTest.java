package stack.moaticket.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.application.component.register.AlarmEmitterRegister;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

class AlarmServiceTest {
    private AlarmService alarmService;
    private AlarmEmitterRegister alarmEmitterRegister;

    @BeforeEach
    void setUp() {
        alarmEmitterRegister = mock(AlarmEmitterRegister.class);
        alarmService = new AlarmService(alarmEmitterRegister);
    }

    @Test
    @DisplayName("구독한 Emitter가 하나도 없을 때 전송하면 연결 끊김 상태로 분류된다.")
    void noSubscriber() {
        // given
        final Long alarmId = 1L;
        final Long memberId = 10L;
        final String concertName = "Concert_A";

        SessionStartAlarm alarm = mockAlarm(alarmId, memberId, concertName);
        given(alarmEmitterRegister.getSseEmitters(memberId)).willReturn(List.of());

        // when
        var results = alarmService.sendConcertStartInform(List.of(alarm));

        // then
        var succeeded = results.get(0);
        var disconnected = results.get(1);

        assertThat(succeeded).isEmpty();
        assertThat(disconnected).containsExactly(alarm);
    }

    @Test
    @DisplayName("구독한 Emitter 중 하나라도 전송을 시도하면 전송으로 분류된다. (실패 여부는 알 수 없음)")
    void partialSuccess() throws Exception {
        // given
        final Long alarmId = 1L;
        final Long memberId = 10L;
        final String concertName = "Concert_A";

        SessionStartAlarm alarm = mockAlarm(alarmId, memberId, concertName);

        SseEmitter pass = mock(SseEmitter.class);
        SseEmitter fail = mock(SseEmitter.class);

        willThrow(new IOException("fail")).given(fail).send(any(SseEmitter.SseEventBuilder.class));
        willDoNothing().given(pass).send(any(SseEmitter.SseEventBuilder.class));

        given(alarmEmitterRegister.getSseEmitters(memberId)).willReturn(List.of(pass, fail));

        // when
        var results = alarmService.sendConcertStartInform(List.of(alarm));

        // then
        var succeeded = results.get(0);
        var failed = results.get(1);

        assertThat(succeeded).containsExactly(alarm);
        assertThat(failed).isEmpty();
    }

    /* Helper */
    private SessionStartAlarm mockAlarm(Long alarmId, Long memberId, String concertName) {
        SessionStartAlarm alarm = mock(SessionStartAlarm.class);
        Member member = mock(Member.class);
        Session session = mock(Session.class);
        Concert concert = mock(Concert.class);
        SessionStartAlarmType type = mock(SessionStartAlarmType.class);

        given(alarm.getId()).willReturn(alarmId);
        given(alarm.getMember()).willReturn(member);
        given(member.getId()).willReturn(memberId);

        given(alarm.getSession()).willReturn(session);
        given(session.getConcert()).willReturn(concert);
        given(concert.getName()).willReturn(concertName);

        given(alarm.getAlarmAt()).willReturn(LocalDateTime.of(2000, 1, 1, 0, 0));
        given(session.getDate()).willReturn(LocalDateTime.of(2000, 1, 1, 0, 0));

        given(alarm.getType()).willReturn(type);
        given(alarm.getType().getPrefix()).willReturn("prefix_");
        given(alarm.getType().getName()).willReturn("name");

        return alarm;
    }
}
