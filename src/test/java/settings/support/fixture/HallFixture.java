package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import settings.support.util.TestUtil;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.hall.repository.HallRepository;
import stack.moaticket.domain.hall.type.HallState;
import stack.moaticket.domain.hall.type.HallType;

public class HallFixture extends BaseFixture<Hall, Long> {
    private final HallRepository hallRepository;

    public HallFixture(HallRepository hallRepository) {
        this.hallRepository = hallRepository;
    }

    @Override
    protected JpaRepository<Hall, Long> repo() {
        return hallRepository;
    }

    @Transactional
    public Hall create() {
        return save(Hall.builder()
                .name(TestUtil.uniqueString("name"))
                .type(HallType.SMALL)
                .state(HallState.AVAILABLE)
                .build());
    }
}
