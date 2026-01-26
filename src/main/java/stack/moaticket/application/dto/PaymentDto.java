package stack.moaticket.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.payment.type.PaymentState;

import java.time.LocalDateTime;

public abstract class PaymentDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmRequest {
        private String orderId;
        private String paymentKey;
        private long amount;
    }

    @Getter
    @Builder
    public static class ConfirmResponse {
        private Long paymentId;
        private String orderId;
        private PaymentState paymentState;
        private LocalDateTime paidAt;
        private long amount;
        private String orderName;
    }


}
