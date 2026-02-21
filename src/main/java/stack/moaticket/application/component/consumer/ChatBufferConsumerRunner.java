package stack.moaticket.application.component.consumer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stack.moaticket.application.model.ChatShardBufferMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBufferConsumerRunner {

    private final ChatBulkProperties properties;

    @PostConstruct
    public void start() {
        for(int i=1; i<=properties.chatMessageBulk().shardCount(); i++) {
            log.info("ChatBufferConsumerRunner : ChatBuffer {} is running", i);
            ChatShardBufferMap.put(i);
        }
    }

}
