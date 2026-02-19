package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.service.ChatMessageService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChattingFacade {

    private final ChatMessageService chatMessageService;
    private final SimpMessageSendingOperations messagingTemplate;


    public void saveAndSend(String content, Long memberId, String playbackId, LocalDateTime sendTime, String memberNickname) {
        ChattingDto.Response response = ChattingDto.Response.builder()
                .message(content)
                .timeStamp(sendTime)
                .senderNickname(memberNickname)
                .build();
        messagingTemplate.convertAndSend("/sub/" + playbackId + "/messages", response);

        chatMessageService.addToBuffer(content, memberId, playbackId, sendTime, memberNickname);

    }


    public List<ChattingDto.Response> getChatHistory(String playbackId, Long lastSeenId, int size) {
        List<ChatMessage> chatMessages;
        if (lastSeenId == null) {
            chatMessages = chatMessageService.getChatHistoryFirst(playbackId, size);
        }
        else {
            chatMessages = chatMessageService.getChatHistory(playbackId, lastSeenId, size);
        }
        return chatMessages.stream()
                .map(ChattingDto.Response::toResponse)
                .collect(Collectors.toList());
    }
}
