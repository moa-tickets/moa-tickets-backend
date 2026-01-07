package stack.moaticket.system.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossConfirmResponse {
    private String paymentKey;
    private String orderId;
    private String status;      // 예: DONE
    private String approvedAt;  // ISO-8601
    private Long totalAmount;   // v1 confirm 응답에 존재하는 경우 금액 검증용
}
