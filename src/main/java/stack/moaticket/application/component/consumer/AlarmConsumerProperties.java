package stack.moaticket.application.component.consumer;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.server.consumer")
public record AlarmConsumerProperties(
        ConsumerConfig ticketRelease
) {
    public record ConsumerConfig(@Min(1) int consumerThread,
                                 @Min(1) int pelThread,
                                 @Min(200) int limit,
                                 @Min(200) long backoffMillis) {}
}
