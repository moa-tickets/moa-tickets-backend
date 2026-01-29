package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session.repository.SessionRepository;

import java.time.LocalDateTime;

public class SessionFixture extends BaseFixture<Session, Long> {
    private final SessionRepository sessionRepository;

    public SessionFixture(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected JpaRepository<Session, Long> repo() {
        return sessionRepository;
    }

    @Transactional
    public Session create(Concert concert) {
        LocalDateTime now = LocalDateTime.now();

        return save(Session.builder()
                .price(10)
                .date(now)
                .concert(concert)
                .build());
    }
}
