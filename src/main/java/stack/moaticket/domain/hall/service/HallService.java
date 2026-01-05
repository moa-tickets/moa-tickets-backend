package stack.moaticket.domain.hall.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.HallDto;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.hall.repository.HallRepository;
import stack.moaticket.domain.hall.repository.HallRepositoryQueryDsl;
import stack.moaticket.domain.hall.type.HallState;
import stack.moaticket.domain.hall.type.HallType;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.List;

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

    public HallDto.HallResponse getHallById(Long id) {

        Hall hall = hallRepositoryQueryDsl.getHall(id);

        if (hall == null){
            throw new MoaException(MoaExceptionType.NOT_FOUND); //TODO
        }

        return HallDto.HallResponse.builder()
                .hallName(hall.getName())
                .hallState(hall.getState())
                .hallType(hall.getType())
                .build();
    }

    public List<HallDto.HallResponse> getHalls(){
        List<Hall> hallList = hallRepositoryQueryDsl.getAllHall();

        return hallList.stream()
                .map(h -> HallDto.HallResponse.builder()
                        .hallName(h.getName())
                        .hallType(h.getType())
                        .hallState(h.getState())
                        .build())
                .toList();
    }


}
