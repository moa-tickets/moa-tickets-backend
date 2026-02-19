package stack.moaticket.system.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import stack.moaticket.domain.hall.service.HallService;
import stack.moaticket.domain.hall.type.HallType;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevHallSeed implements ApplicationRunner {

    private final HallService hallService;

    @Override
    public void run(ApplicationArguments args) {
        hallService.upsertHall("MOA HALL SMALL", HallType.SMALL);
        hallService.upsertHall("MOA HALL MEDIUM", HallType.MEDIUM);
        hallService.upsertHall("MOA HALL LARGE", HallType.LARGE);
        hallService.upsertHall("MOA HALL XLARGE", HallType.XLARGE);
        hallService.upsertHall("MOA HALL XXLARGE", HallType.XXLARGE);
    }
}
