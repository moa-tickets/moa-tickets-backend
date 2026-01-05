package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.hall.type.HallState;
import stack.moaticket.domain.hall.type.HallType;

public class HallDto {

    @Getter
    @NoArgsConstructor
    public static class HallRequest{
        private String hallName;
        private HallType hallType;
    }

    @Getter
    @Builder
    public static class HallResponse{
        private String hallName;
        private HallType hallType;
        private HallState hallState;
    }
}
