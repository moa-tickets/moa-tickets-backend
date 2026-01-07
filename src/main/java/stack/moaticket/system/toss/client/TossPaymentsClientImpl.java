package stack.moaticket.system.toss.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import stack.moaticket.system.toss.dto.TossConfirmRequest;
import stack.moaticket.system.toss.dto.TossConfirmResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TossPaymentsClientImpl implements TossPaymentsClient {

    private final WebClient webClient;
    private final String secretKey;

    public TossPaymentsClientImpl(
            @Qualifier("tossWebClient") WebClient webClient,
            @Value("${toss.secret-key}") String secretKey) {
        this.webClient = webClient;
        this.secretKey = secretKey;
    }

    @Override
    public TossConfirmResponse confirm(TossConfirmRequest request) {
        return webClient.post()
                .uri("/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, basicAuth(secretKey))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }

    private String basicAuth(String secretKey) {
        // secretKey + ":" 를 base64 인코딩
        String raw = secretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
