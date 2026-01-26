package stack.moaticket.domain.chat_message.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.chat_message.entity.ChatMessage;

import java.util.List;

import static stack.moaticket.domain.chat_message.entity.QChatMessage.chatMessage;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryQueryDsl {

    private final JPAQueryFactory jpaQueryFactory;

    public List<ChatMessage> getChatHistory(String playbackId, Long lastSeenId, int size){
        List<ChatMessage> chatMessages =  jpaQueryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatroomId.eq(playbackId),
                        chatMessage.id.lt(lastSeenId))
                .orderBy(chatMessage.timestamp.desc())
                .limit(size)
                .fetch();
        return chatMessages;
    }
    public List<ChatMessage> getChatHistoryFirst(String playbackId, int size){
        List<ChatMessage> chatMessages =  jpaQueryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatroomId.eq(playbackId))
                .orderBy(chatMessage.timestamp.desc())
                .limit(size)
                .fetch();
        return chatMessages;
    }


}
