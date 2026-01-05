package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class PaymentDto {

    @Getter
    @NoArgsConstructor
    public static class PrepareRequest {
        private String holdToken;
    }

    @Getter
    @Builder
    public static class PrepareResponse {
        private String orderId;
        private String orderName;
        private long amount;
    }
}
