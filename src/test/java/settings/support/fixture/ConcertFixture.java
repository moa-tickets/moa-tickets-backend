package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import settings.support.util.TestUtil;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.concert.repository.ConcertRepository;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;

public class ConcertFixture extends BaseFixture<Concert, Long> {
    private final ConcertRepository concertRepository;

    public ConcertFixture(ConcertRepository concertRepository) {
        this.concertRepository = concertRepository;
    }

    @Override
    protected JpaRepository<Concert, Long> repo() {
        return concertRepository;
    }

    @Transactional
    public Concert create(Member member, Hall hall) {
        LocalDateTime now = LocalDateTime.now();

        return save(Concert.builder()
                .name(TestUtil.uniqueString("name"))
                .age(0)
                .duration(TestUtil.uniqueString("duration"))
                .bookingOpen(now)
                .start(now)
                .end(now)
                .member(member)
                .hall(hall)
                .build());
    }
}
