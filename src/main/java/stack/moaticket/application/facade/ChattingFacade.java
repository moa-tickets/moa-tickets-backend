package stack.moaticket.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChattingFacade {

    private final ChatMessageService chatMessageService;
    private final MemberService memberService;
    private final SimpMessageSendingOperations messagingTemplate;

    @Transactional
    public void saveAndSend(String content, Long memberId, String playbackId, LocalDateTime sendTime) {

        Member member = memberService.getByIdOrThrow(memberId);

        ChatMessage chatMessage = chatMessageService.saveMessage(content, member, playbackId, sendTime);

        ChattingDto.Response response = ChattingDto.Response.toResponse(chatMessage);

        //TODO: w/s 전송 부분이 속도가 느리면.. 전체 트랜잭션의 생명주기는 어떻게 되나요? 분리가 필요하다고 생각이 드나요?
        messagingTemplate.convertAndSend("/sub/" + playbackId + "/messages", response);
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
