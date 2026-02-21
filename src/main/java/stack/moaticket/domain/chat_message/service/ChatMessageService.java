package stack.moaticket.domain.chat_message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import stack.moaticket.application.model.ChatShardBuffer;
import stack.moaticket.application.model.ChatShardBufferMap;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.repository.ChatMessageBulkRepository;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepositoryQueryDsl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepositoryQueryDsl chatMessageRepositoryQueryDsl;
    private final ChatMessageBulkRepository chatMessageBulkRepository;


    @Async(value = "chatToBufferExecutor")
    public void addToBuffer(String content, Long memberId, String playbackId, LocalDateTime sendTime, String memberNickname) {
        ChatMessage chatMessage = ChatMessage.builder()
                .chatroomId(playbackId)
                .content(content)
                .memberId(memberId)
                .timestamp(sendTime)
                .nickname(memberNickname)
                .build();
        switch ((int) (chatMessage.getMemberId() % 4) + 1) {
            case 1:
                ChatShardBufferMap.getBuffer(1).getActive().add(chatMessage);
                break;
            case 2:
                ChatShardBufferMap.getBuffer(2).getActive().add(chatMessage);
                break;
            case 3:
                ChatShardBufferMap.getBuffer(3).getActive().add(chatMessage);
                break;
            case 4:
                ChatShardBufferMap.getBuffer(4).getActive().add(chatMessage);
                break;
        }
    }

    public void saveBulk(ChatShardBuffer shard) {
        List<ChatMessage> snapShot = new ArrayList<>(shard.getInactive());
        shard.getInactive().clear();
        if (!snapShot.isEmpty()) {
            chatMessageBulkRepository.saveAllMessages(snapShot);
        }
    }

    public List<ChatMessage> getChatHistoryFirst(String playbackId, int size) {
        return chatMessageRepositoryQueryDsl.getChatHistoryFirst(playbackId, size);
    }
    public List<ChatMessage> getChatHistory(String playbackId, Long lastSeenId, int size) {
        return chatMessageRepositoryQueryDsl.getChatHistory(playbackId, lastSeenId, size);
    }
}
