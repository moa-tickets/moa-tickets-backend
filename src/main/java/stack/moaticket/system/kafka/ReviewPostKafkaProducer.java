package stack.moaticket.system.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.ReviewDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReviewPostKafkaProducer {
    // Spring Kafka가 제공하는 템플릿 클래스 — DI로 주입받음
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendReviewPost(ReviewDto.SpringReviewItemDto dto)  {
        try {
            String message = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("reviews", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
