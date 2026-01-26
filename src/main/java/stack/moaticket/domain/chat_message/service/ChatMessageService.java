package stack.moaticket.domain.chat_message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepository;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepositoryQueryDsl;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageRepositoryQueryDsl chatMessageRepositoryQueryDsl;

    public ChatMessage saveMessage(String content, Member member, String playbackId, LocalDateTime sendTime) {
        return chatMessageRepository.save(
                ChatMessage.builder()
                        .chatroomId(playbackId)
                        .content(content)
                        .nickname(member.getNickname())
                        .member(member)
                        .timestamp(sendTime)
                .build());
    }

    public List<ChatMessage> getChatHistoryFirst(String playbackId, int size) {
        return chatMessageRepositoryQueryDsl.getChatHistoryFirst(playbackId, size);
    }
    public List<ChatMessage> getChatHistory(String playbackId, Long lastSeenId, int size) {
        return chatMessageRepositoryQueryDsl.getChatHistory(playbackId, lastSeenId, size);
    }
}
