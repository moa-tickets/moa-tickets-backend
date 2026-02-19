package stack.moaticket.domain.chat_message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.repository.ChatMessageBulkRepository;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageBulkService {
    private final ChatMessageBulkRepository chatMessageBulkRepository;
    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private static final int BATCH_SIZE = 100;

    @Transactional
    public void saveBulk(Queue<ChatMessage> buffer) {
        if (buffer.isEmpty() || !isSaving.compareAndSet(false, true)) return;

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
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR); // @Transactional 롤백을 위해 재던지기
            } finally {
                isSaving.set(false);
            }
        }
    }

}
