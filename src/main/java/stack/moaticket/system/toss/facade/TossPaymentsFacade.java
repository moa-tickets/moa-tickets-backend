package stack.moaticket.system.toss.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.toss.client.TossPaymentsClient;
import stack.moaticket.system.toss.dto.*;

@Component
@RequiredArgsConstructor
public class TossPaymentsFacade {

    private final TossPaymentsClient tossPaymentsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TossConfirmResponse confirm(String paymentKey, String orderId, long amount) {
        try {
            TossConfirmResponse res = tossPaymentsClient.confirm(new TossConfirmRequest(paymentKey, orderId, amount));

            // (선택) 응답 검증: orderId / amount
            if (res == null || res.getOrderId() == null || !res.getOrderId().equals(orderId)) {
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
            if (res.getTotalAmount() != null && res.getTotalAmount().longValue() != amount) {
                throw new MoaException(MoaExceptionType.INVALID_PAYMENT_AMOUNT);
            }

            return res;
        } catch (WebClientResponseException e) {
            TossErrorResponse err = tryParseError(e.getResponseBodyAsString());
            // TODO: err.getCode()에 따라 더 세밀히 매핑 가능
            throw new MoaException(MoaExceptionType.CONFLICT, err != null ? err.getMessage() : "Toss confirm failed");
        }
    }

//    public TossCancelResponse cancel(String paymentKey, long amount, String reason) {
//        try {
//            TossCancelResponse res = tossPaymentsClient.cancel(new TossCancelRequest(paymentKey, amount, reason));
//            return res;
//
//        }catch (WebClientResponseException e){
//            TossErrorResponse err = tryParseError(e.getResponseBodyAsString());
//            throw new MoaException(MoaExceptionType.CONFLICT, err != null ? err.getMessage() : "Toss confirm failed");
//        }
//    }

    private TossErrorResponse tryParseError(String body) {
        try {
            if (body == null || body.isBlank()) return null;
            return objectMapper.readValue(body, TossErrorResponse.class);
        } catch (Exception ignore) {
            return null;
        }
    }
}
