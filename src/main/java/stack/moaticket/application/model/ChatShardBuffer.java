package stack.moaticket.application.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import stack.moaticket.domain.chat_message.entity.ChatMessage;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
public class ChatShardBuffer {
    private final Queue<ChatMessage> buffer1;
    private final Queue<ChatMessage> buffer2;
    @Getter
    private final int shardNum;
    private boolean flag;
    @Getter
    private Long lastUpdatedTime = System.currentTimeMillis();


    public ChatShardBuffer(int shardNum) {
        this.shardNum = shardNum;
        this.buffer1 = new ConcurrentLinkedQueue<>();
        this.buffer2 = new ConcurrentLinkedQueue<>();
        this.flag = true;
    }

    public Queue<ChatMessage> getActive() {
        return flag ? buffer1 : buffer2;
    }

    public Queue<ChatMessage> getInactive() {
        return flag ? buffer2 : buffer1;
    }

    public int getActiveSize() {
        return flag ? buffer1.size() : buffer2.size();
    }

    public void swap() {
        flag = !flag;
        this.lastUpdatedTime = System.currentTimeMillis();
    }

    public boolean isFull(int threshold) {
        return getActiveSize() >= threshold;
    }
}
