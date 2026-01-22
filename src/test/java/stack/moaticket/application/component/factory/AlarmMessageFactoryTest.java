package stack.moaticket.application.component.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.application.port.AlarmMessage;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class AlarmMessageFactoryTest {
    @InjectMocks private AlarmMessageFactory alarmMessageFactory;

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
    void test() {
        // given
        SessionStartAlarmMetaDto alarm = mock(SessionStartAlarmMetaDto.class);
        given(alarm.type()).willReturn(SessionStartAlarmType.ON_HOUR);

        // when
        AlarmMessage message = alarmMessageFactory.sessionStart(alarm);

        // then
        assertThat(message.key()).isEqualTo("SS_ON_HOUR");
        assertThat(message.payload()).isSameAs(alarm);
    }
}
