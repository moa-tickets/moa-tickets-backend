package stack.moaticket.application.model;

import java.util.HashMap;
import java.util.Map;

public class ChatShardBufferMap {
    private ChatShardBufferMap() {}

    public static final Map<Integer, ChatShardBuffer> bufferMap = new HashMap<>();

    public static ChatShardBuffer getBuffer(int shardNum) {
        return bufferMap.get(shardNum);
    }
    public static void put(int shardNum) {
        bufferMap.put(shardNum, new ChatShardBuffer(shardNum));
    }
}
