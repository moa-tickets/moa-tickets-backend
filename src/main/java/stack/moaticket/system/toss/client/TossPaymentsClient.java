package stack.moaticket.system.toss.client;

import stack.moaticket.system.toss.dto.TossConfirmRequest;
import stack.moaticket.system.toss.dto.TossConfirmResponse;

public interface TossPaymentsClient {
    TossConfirmResponse confirm(TossConfirmRequest request);
//    TossConfirmResponse cancel(String paymentKey);
}
