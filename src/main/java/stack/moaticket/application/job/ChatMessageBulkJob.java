package stack.moaticket.application.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.component.consumer.ChatBulkProperties;
import stack.moaticket.application.model.ChatShardBuffer;
import stack.moaticket.application.model.ChatShardBufferMap;
import stack.moaticket.domain.chat_message.service.ChatMessageService;

@Slf4j
@Service
public class ChatMessageBulkJob {
    private final ChatMessageService chatMessageService;
    private final ThreadPoolTaskExecutor executor;

    private final ChatBulkProperties properties;

    public ChatMessageBulkJob(ChatMessageService chatMessageService,
                              @Qualifier("bufferToDbExecutor") ThreadPoolTaskExecutor executor,
                              ChatBulkProperties properties) {
        this.chatMessageService = chatMessageService;
        this.executor = executor;
        this.properties = properties;
    }


    public void run() {
        for(int i = 1; i <= properties.chatMessageBulk().shardCount(); i++) {
            ChatShardBuffer shard = ChatShardBufferMap.getBuffer(i);
            if(shard.isFull(properties.chatMessageBulk().threshold()) ||
                    (System.currentTimeMillis() - shard.getLastUpdatedTime() > properties.chatMessageBulk().flushTimeout())) {
                shard.swap();
                executor.execute(() -> chatMessageService.saveBulk(shard));
            }
        }
    }

}
