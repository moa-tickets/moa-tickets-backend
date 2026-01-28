package stack.moaticket.application.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.application.facade.ChattingFacade;
import stack.moaticket.system.exception.MoaException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompControllerTest {

    @Mock
    ChattingFacade chattingFacade;
    @InjectMocks StompController stompController;


    @DisplayName("메세지 보내기 성공")
    @Test
    void successSendMessage() {
        //given
        Map<String,Object> map = new HashMap<>();
        ChattingDto.Request request = new ChattingDto.Request();
        String playbackId = "playbackId";
        map.put("memberId", 1L);
        map.put("roomId", "playbackId");
        map.put("sendTime", LocalDateTime.now());
        //when
        stompController.sendMessage(map, request, playbackId);

        //then
        verify(chattingFacade, times(1)).saveAndSend(any(), anyLong(), anyString(), any());
    }


    @DisplayName("memberId null 처리")
    @Test
    void failedSendMessageMemberIdNull() {
        //given
        Map<String,Object> map = new HashMap<>();
        ChattingDto.Request request = new ChattingDto.Request();
        String playbackId = "playbackId";
        Long memberId = null;
        LocalDateTime now = LocalDateTime.now();
        String subscribedRoom = "subcribedRoom";
        //when
        assertThatThrownBy(() -> stompController.sendMessage(map, request, playbackId))
                .isInstanceOf(MoaException.class)
                .hasMessage("인증되지 않은 사용자입니다.");
        //then
        verify(chattingFacade, never()).saveAndSend(any(), anyLong(), anyString(), any());
    }


    @DisplayName("메세지 보내기 실패 구독하지 않은 채팅방에 채팅")
    @Test
    void failedSendMessageByUnsubscribe() {
        //given
        Map<String,Object> map = new HashMap<>();
        ChattingDto.Request request = new ChattingDto.Request();
        String playbackId = "playbackId";
        map.put("memberId", 1L);
        map.put("roomId", "roomId");
        map.put("sendTime", LocalDateTime.now());
        //when
        assertThatThrownBy(() -> stompController.sendMessage(map, request, playbackId))
                .isInstanceOf(MoaException.class)
                .hasMessage("권한이 없습니다.");

        //then
        verify(chattingFacade, never()).saveAndSend(any(), anyLong(), anyString(), any());
    }
}