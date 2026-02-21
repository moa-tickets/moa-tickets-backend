package stack.moaticket.application.component.consumer;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stack.moaticket.application.model.ChatShardBufferMap;

@Slf4j
@Component
public class ChatBufferConsumerRunner {

    private static final int SHARD_COUNT = 4;

    @PostConstruct
    public void start() {
        for(int i=1; i<=SHARD_COUNT; i++) {
            log.info("ChatBufferConsumerRunner : ChatBuffer {} is running", i);
            ChatShardBufferMap.put(i);
        }
    }

}
