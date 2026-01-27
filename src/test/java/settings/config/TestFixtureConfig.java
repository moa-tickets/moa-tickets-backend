package settings.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import settings.support.fixture.*;
import stack.moaticket.domain.concert.repository.ConcertRepository;
import stack.moaticket.domain.hall.repository.HallRepository;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.session.repository.SessionRepository;
import stack.moaticket.domain.session_start_alarm.repository.SessionStartAlarmRepository;

@Profile("test")
@TestConfiguration
public class TestFixtureConfig {

    @Bean
    public MemberFixture memberFixture(MemberRepository memberRepository) {
        return new MemberFixture(memberRepository);
    }

    @Bean
    public HallFixture hallFixture(HallRepository hallRepository) {
        return new HallFixture(hallRepository);
    }

    @Bean
    public ConcertFixture concertFixture(ConcertRepository concertRepository) {
        return new ConcertFixture(concertRepository);
    }

    @Bean
    public SessionFixture sessionFixture(SessionRepository sessionRepository) {
        return new SessionFixture(sessionRepository);
    }

    @Bean
    public SessionStartAlarmFixture sessionStartAlarmFixture(SessionStartAlarmRepository sessionStartAlarmRepository) {
        return new SessionStartAlarmFixture(sessionStartAlarmRepository);
    }
}
