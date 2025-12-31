package stack.moaticket.domain.hall.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.hall.repository.HallRepository;
import stack.moaticket.domain.hall.repository.HallRepositoryQueryDsl;
import stack.moaticket.domain.hall.type.HallState;
import stack.moaticket.domain.hall.type.HallType;

@Service
@RequiredArgsConstructor
public class HallService {

    private final HallRepository hallRepository;
    private final HallRepositoryQueryDsl hallRepositoryQueryDsl;

    @Transactional
    public Hall upsertHall(String name, HallType type) {
        Hall existing = hallRepositoryQueryDsl.getHallByName(name);

        if (existing != null) {
            existing.setType(type);
            existing.setState(HallState.AVAILABLE);
            return existing;
        }

        Hall hall = Hall.builder()
                .name(name)
                .type(type)
                .state(HallState.AVAILABLE)
                .build();

        return hallRepository.save(hall);
    }
}
