package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.hall.type.HallState;
import stack.moaticket.domain.hall.type.HallType;

public class HallDto {

    //Request
    @Getter
    @NoArgsConstructor
    public static class HallRequest{
        private String hallName;
        private HallType hallType;
    }

    //HallResponse에 이 세가지가 모두 필요할지...
    @Getter
    @Builder
    public static class HallResponse{
        private String hallName;
        private HallType hallType;
        private HallState hallState;
    }
}
