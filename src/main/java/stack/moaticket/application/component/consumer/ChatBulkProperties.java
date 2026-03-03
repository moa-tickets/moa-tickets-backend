package stack.moaticket.application.component.consumer;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.server.consumer.chat")
public record ChatBulkProperties (
        ConsumerConfig chatMessageBulk
) {
    public record ConsumerConfig(@Min(1) int shardCount,
                                 @Min(1) int threshold,
                                 @Min(1) Long flushTimeout) {}
}

