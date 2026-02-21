package stack.moaticket.application.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.model.ChatShardBuffer;
import stack.moaticket.application.model.ChatShardBufferMap;
import stack.moaticket.domain.chat_message.service.ChatMessageService;

@Slf4j
@Service
public class ChatMessageBulkJob {
    private final ChatMessageService chatMessageService;
    private final ThreadPoolTaskExecutor executor;

    private static final int SHARD_COUNT = 4;

    public ChatMessageBulkJob(ChatMessageService chatMessageService,
                              @Qualifier("bufferToDbExecutor") ThreadPoolTaskExecutor executor) {
        this.chatMessageService = chatMessageService;
        this.executor = executor;
    }


    public void run() {
        for(int i = 1; i <= SHARD_COUNT; i++) {
            ChatShardBuffer shard = ChatShardBufferMap.getBuffer(i);
            if(shard.isFull()) {
                shard.swap();
                executor.execute(() -> chatMessageService.saveBulk(shard));
            }
        }
    }

}
