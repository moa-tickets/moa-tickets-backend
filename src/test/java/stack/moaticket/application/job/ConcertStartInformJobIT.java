package stack.moaticket.application.job;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import settings.config.TestFixtureConfig;
import settings.support.fixture.*;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmType;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(
        properties = {
                "app.server.scheduler.session-start=false",
                "app.server.scheduler.ticket-release=false"
        })
@Import(TestFixtureConfig.class)
public class  ConcertStartInformJobIT {
    @Autowired ConcertStartInformJob job;

    // Fixtures
    @Autowired MemberFixture memberFixture;
    @Autowired HallFixture hallFixture;
    @Autowired ConcertFixture concertFixture;
    @Autowired SessionFixture sessionFixture;
    @Autowired SessionStartAlarmFixture sessionStartAlarmFixture;

    @MockitoSpyBean AlarmService alarmService;

    @AfterEach
    void clear() {
        sessionStartAlarmFixture.clear();
        sessionFixture.clear();
        concertFixture.clear();
        hallFixture.clear();
        memberFixture.clear();
    }

    @Test
    @DisplayName("임계 시간이 지나지 않은 PENDING 알림이 후보로 추출되면 PROCESSED로 상태가 변경되고, 알림 발송을 시도한다.")
    void pendingNotificationNotExceededThenChangesProcessedAndSend() {
        // given
        LocalDateTime alarmAt = LocalDateTime.now().minus(Duration.ofSeconds(5));

        Member m = memberFixture.create();
        Hall h = hallFixture.create();
        Concert c = concertFixture.create(m, h);
        Session s = sessionFixture.create(c);

        SessionStartAlarm ssa1 = sessionStartAlarmFixture.create(m, s, alarmAt, SessionStartAlarmType.LEFT_10);
        SessionStartAlarm ssa2 = sessionStartAlarmFixture.create(m, s, alarmAt, SessionStartAlarmType.ON_HOUR);

        // when
        job.runEpoch();

        // then
        SessionStartAlarm r1 = sessionStartAlarmFixture.findById(ssa1.getId());
        SessionStartAlarm r2 = sessionStartAlarmFixture.findById(ssa2.getId());
        assertThat(r1.getState()).isEqualTo(SessionStartAlarmState.PROCESSED);
        assertThat(r2.getState()).isEqualTo(SessionStartAlarmState.PROCESSED);
        assertThat(r1.getType()).isEqualTo(SessionStartAlarmType.LEFT_10);
        assertThat(r2.getType()).isEqualTo(SessionStartAlarmType.ON_HOUR);

        then(alarmService).should(times(1))
                .sendConcertStartInform(argThat(list -> list != null && list.size() == 2));
    }
}