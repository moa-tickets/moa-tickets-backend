package stack.moaticket.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.application.facade.ChattingFacade;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.service.ChatMessageService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ChattingFacadeTest {

    @Mock
    MemberService memberService;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    ChatMessageService chatMessageService;
    @InjectMocks
    ChattingFacade chattingFacade;

    @DisplayName("채팅 저장 성공 및 전송")
    @Test
    void chattingSaveAndSend(){
        //given
        Long memberId = 1L;
        String playbackId = "playbackId";
        String content = "content";
        LocalDateTime sendTime = LocalDateTime.now();

        Member member = mock(Member.class);
        when(memberService.getByIdOrThrow(memberId)).thenReturn(member);

        ChatMessage saved = mock(ChatMessage.class);
        when(chatMessageService.saveMessage(content, member, playbackId, sendTime)).thenReturn(saved);
        //when
        chattingFacade.saveAndSend(content, memberId, playbackId, sendTime);

        //then
        verify(memberService).getByIdOrThrow(memberId);
        verify(chatMessageService).saveMessage(content, member, playbackId, sendTime);

        verify(messagingTemplate).convertAndSend(
                eq("/sub/" + playbackId + "/messages"),
                any(ChattingDto.Response.class)
        );

    }


    @DisplayName("채팅 리스트 가져오기 테스트")
    @Test
    void getChatHistory(){
        //given
        ChatMessage chat1 = mock(ChatMessage.class);
        ChatMessage chat2 = mock(ChatMessage.class);
        String playbackId = "playbackId";
        int size = 2;
        Long lastSeenId = 1L;

        when(chatMessageService.getChatHistory(playbackId, lastSeenId, size))
                .thenReturn(Arrays.asList(chat1, chat2));

        //when
        List<ChattingDto.Response> chatHistory = chattingFacade.getChatHistory(playbackId, lastSeenId, size
        );

        //then
        verify(chatMessageService).getChatHistory(playbackId, lastSeenId, size);
        verify(chatMessageService, never()).getChatHistoryFirst(anyString(), anyInt());

        assertThat(chatHistory).hasSize(2);
    }

    @DisplayName("채팅 리스트 처음 가져오기 테스트")
    @Test
    void getChatHistoryFirst(){
        //given
        ChatMessage chat1 = mock(ChatMessage.class);
        ChatMessage chat2 = mock(ChatMessage.class);
        String playbackId = "playbackId";
        int size = 2;

        when(chatMessageService.getChatHistoryFirst("playbackId", 2))
                .thenReturn(Arrays.asList(chat1, chat2));

        //when
        List<ChattingDto.Response> chatHistory = chattingFacade.getChatHistory("playbackId", null, 2);

        //then
        verify(chatMessageService, never()).getChatHistory(anyString(), anyLong(), anyInt());
        verify(chatMessageService).getChatHistoryFirst("playbackId", 2);
    }

    @DisplayName("member == null일시 전송 실패")
    @Test
    void test(){
        Long memberId = 999L;
        when(memberService.getByIdOrThrow(memberId))
                .thenThrow(new MoaException(MoaExceptionType.MEMBER_NOT_FOUND));

        assertThatThrownBy(() ->
                chattingFacade.saveAndSend("hi", memberId, "roomA", LocalDateTime.now())
        ).isInstanceOf(MoaException.class);

        verify(chatMessageService, never()).saveMessage(any(), any(), any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), Optional.ofNullable(any()));

    }






}