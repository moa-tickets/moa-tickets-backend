package stack.moaticket.domain.chat_message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.repository.ChatMessageBulkRepository;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepository;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepositoryQueryDsl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageRepositoryQueryDsl chatMessageRepositoryQueryDsl;
    private final ChatMessageBulkRepository chatMessageBulkRepository;

    private final Queue<ChatMessage> buffer = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 100;


    @Async("chatAsyncExecutor")
    public void addToBuffer(String content, Long memberId, String playbackId, LocalDateTime sendTime, String memberNickname) {
        ChatMessage chatMessage = ChatMessage.builder()
                .chatroomId(playbackId)
                .content(content)
                .memberId(memberId)
                .timestamp(sendTime)
                .nickname(memberNickname)
                .build();
        buffer.add(chatMessage);
        if (buffer.size() >= BATCH_SIZE) {
            saveBulk();
        }
    }
    @Transactional
    public void saveBulk() {
        if (buffer.isEmpty()) return;

        List<ChatMessage> batchList = new ArrayList<>();
        while (!buffer.isEmpty() && batchList.size() < BATCH_SIZE) {
            ChatMessage chatMessage = buffer.poll();
            if (chatMessage == null) {
                break;
            }
            batchList.add(chatMessage);
        }

        if (!batchList.isEmpty()) {
            try {
                chatMessageBulkRepository.saveAllMessages(batchList);
            } catch (Exception e) {
                // 실패 시 buffer에 다시 반환
                buffer.addAll(batchList);
                log.error("bulk save 실패, buffer에 {} 건 반환", batchList.size(), e);
                throw e; // @Transactional 롤백을 위해 재던지기
            }
        }
    }

    public List<ChatMessage> getChatHistoryFirst(String playbackId, int size) {
        return chatMessageRepositoryQueryDsl.getChatHistoryFirst(playbackId, size);
    }
    public List<ChatMessage> getChatHistory(String playbackId, Long lastSeenId, int size) {
        return chatMessageRepositoryQueryDsl.getChatHistory(playbackId, lastSeenId, size);
    }
}
