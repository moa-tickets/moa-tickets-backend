package stack.moaticket.domain.chat_message.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepository;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepositoryQueryDsl;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    ChatMessageRepository chatMessageRepository;
    @Mock
    ChatMessageRepositoryQueryDsl chatMessageRepositoryQueryDsl;
    @InjectMocks
    ChatMessageService chatMessageService;

    @DisplayName("채팅 저장")
    @Test
    void saveMessageTest(){
        // given
        String content = "hello";
        String playbackId = "roomA";
        LocalDateTime sendTime = LocalDateTime.of(2026, 1, 26, 2, 30);

        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("soonil");

        ChatMessage saved = mock(ChatMessage.class);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        // when
        ChatMessage result = chatMessageService.saveMessage(content, member, playbackId, sendTime);

        // then
        assertThat(result).isSameAs(saved);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(1)).save(captor.capture());

        ChatMessage toSave = captor.getValue();
        assertThat(toSave.getChatroomId()).isEqualTo(playbackId);
        assertThat(toSave.getContent()).isEqualTo(content);
        assertThat(toSave.getNickname()).isEqualTo("soonil");
        assertThat(toSave.getMember()).isSameAs(member);
        assertThat(toSave.getTimestamp()).isEqualTo(sendTime);

        verify(member, times(1)).getNickname();
    }

    @DisplayName("이전 채팅 처음 조회")
    @Test
    void getChatHistoryTestFirst(){
        // given
        String playbackId = "roomA";
        int size = 20;

        ChatMessage m1 = mock(ChatMessage.class);
        ChatMessage m2 = mock(ChatMessage.class);
        List<ChatMessage> expected = List.of(m1, m2);

        when(chatMessageRepositoryQueryDsl.getChatHistoryFirst(playbackId, size))
                .thenReturn(expected);

        // when
        List<ChatMessage> result = chatMessageService.getChatHistoryFirst(playbackId, size);

        // then
        verify(chatMessageRepositoryQueryDsl, times(1))
                .getChatHistoryFirst(playbackId, size);

        assertThat(result).containsExactly(m1, m2);
    }

    @DisplayName("이전 채팅 조회")
    @Test
    void getChatHistoryTest(){
        // given
        String playbackId = "roomA";
        int size = 20;
        Long lastSeenId = 1L;
        ChatMessage m1 = mock(ChatMessage.class);
        ChatMessage m2 = mock(ChatMessage.class);
        List<ChatMessage> expected = List.of(m1, m2);

        when(chatMessageRepositoryQueryDsl.getChatHistory(playbackId, lastSeenId, size))
                .thenReturn(expected);

        // when
        List<ChatMessage> result = chatMessageService.getChatHistory(playbackId, lastSeenId, size);

        // then
        verify(chatMessageRepositoryQueryDsl, times(1))
                .getChatHistory(playbackId, lastSeenId, size);

        assertThat(result).containsExactly(m1, m2);
    }

}