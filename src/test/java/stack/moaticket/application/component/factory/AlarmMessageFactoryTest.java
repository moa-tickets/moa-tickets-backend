package stack.moaticket.application.component.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.system.alarm.core.component.AlarmMessageFactory;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class AlarmMessageFactoryTest {
    private final AlarmMessageFactory alarmMessageFactory = new AlarmMessageFactory();

    @Test
    @DisplayName("연결 메시지는 CONNECT key에 connected payload로 고정 생성된다.")
    void sessionConnectKey() {
        // when
        AlarmMessage message = AlarmMessageFactory.connect();

        // then
        assertThat(message).isEqualTo(new AlarmMessage("CONNECT", "connected"));
    }

    @Test
    @DisplayName("Heartbeat 메시지는 HEARTBEAT key에 ping payload로 고정 생성된다.")
    void sessionHeartbeatKey() {
        // when
        AlarmMessage message = AlarmMessageFactory.heartbeat();

        // then
        assertThat(message).isEqualTo(new AlarmMessage("HEARTBEAT", "ping"));
    }

    @Test
    @DisplayName("세션 시작 알림 타입이 LEFT_10이면 key는 SS_LEFT_10이다.")
    void sessionStartLeft10Key() {
        // given
        SessionStartAlarmMetaDto alarm = mock(SessionStartAlarmMetaDto.class);
        given(alarm.type()).willReturn(SessionStartAlarmType.LEFT_10);

        // when
        AlarmMessage message = alarmMessageFactory.sessionStart(alarm);

        // then
        assertThat(message.key()).isEqualTo("SS_LEFT_10");
        assertThat(message.payload()).isSameAs(alarm);
    }

    @Test
    @DisplayName("세션 시작 알림 타입이 ON_HOUR이면 key는 SS_ON_HOUR이다.")
    void sessionStartOnHourKey() {
        // given
        SessionStartAlarmMetaDto alarm = mock(SessionStartAlarmMetaDto.class);
        given(alarm.type()).willReturn(SessionStartAlarmType.ON_HOUR);

        // when
        AlarmMessage message = alarmMessageFactory.sessionStart(alarm);

        // then
        assertThat(message.key()).isEqualTo("SS_ON_HOUR");
        assertThat(message.payload()).isSameAs(alarm);
    }

    @Test
    @DisplayName("발송할 티켓 홀드 해제 알림이 단일 건수면 key는 TR_SINGLE이다.")
    void ticketReleaseSingleKey() {
        // given
        List<TicketMetaDto> alarmList = List.of(mock(TicketMetaDto.class));

        // when
        AlarmMessage message = alarmMessageFactory.ticketRelease(alarmList);

        // then
        assertThat(message.key()).isEqualTo("TR_SINGLE");
        assertThat(message.payload()).isSameAs(alarmList);
    }

    @Test
    @DisplayName("발송할 티켓 홀드 해제 알림이 여러 건수면 key는 TR_BULK이다.")
    void ticketReleaseBulkKey() {
        // given
        List<TicketMetaDto> alarmList = List.of(
                mock(TicketMetaDto.class),
                mock(TicketMetaDto.class));

        // when
        AlarmMessage message = alarmMessageFactory.ticketRelease(alarmList);

        // then
        assertThat(message.key()).isEqualTo("TR_BULK");
        assertThat(message.payload()).isSameAs(alarmList);
    }

    @Test
    @DisplayName("발송할 티켓 홀드 해제 알림이 0건으로 입력되면 MISMATCH_PARAMETER 오류를 발생시킨다.")
    void ticketReleaseAmountZero() {
        // given
        List<TicketMetaDto> alarmList = List.of();

        // when & then
        assertThatThrownBy(() -> alarmMessageFactory.ticketRelease(alarmList))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);
    }

    @Test
    @DisplayName("발송할 티켓 홀드 해제 알림이 NULL로 입력되면 MISMATCH_PARAMETER 오류를 발생시킨다.")
    void ticketReleaseNull() {
        // when & then
        assertThatThrownBy(() -> alarmMessageFactory.ticketRelease(null))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);
    }
}
