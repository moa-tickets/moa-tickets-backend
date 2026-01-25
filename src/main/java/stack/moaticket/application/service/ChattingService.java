package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepository;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepositoryQueryDsl;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChattingService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepositoryQueryDsl memberRepositoryQueryDsl;
    private final ChatMessageRepositoryQueryDsl chatMessageRepositoryQueryDsl;
    private final SimpMessageSendingOperations messagingTemplate;


    @Transactional
    public void saveAndSend(String content, Long memberId, String playbackId) {

        Member member = memberRepositoryQueryDsl.findById(memberId);
        if (member == null) throw new MoaException(MoaExceptionType.MEMBER_NOT_FOUND);

        ChatMessage chatMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .chatroomId(playbackId)
                        .nickname(member.getNickname())
                        .member(member)
                        .content(content)
                        .build()
        );

        ChattingDto.Response response = ChattingDto.Response.builder()
                .message(chatMessage.getContent())
                .senderNickname(member.getNickname())
                .build();

        //TODO: w/s 전송 부분이 속도가 느리면.. 전체 트랜잭션의 생명주기는 어떻게 되나요? 분리가 필요하다고 생각이 드나요?
        messagingTemplate.convertAndSend("/sub/" + playbackId + "/messages", response);
    }

    public List<ChattingDto.Response> getChatHistory(String playbackId, int page, int size) {
        List<ChatMessage> chatMessages = chatMessageRepositoryQueryDsl.getChatHistory(playbackId, page, size);
        List<ChattingDto.Response> response = chatMessages.stream()
                .map(chatMessage -> ChattingDto.Response.builder()
                        .message(chatMessage.getContent())
                        .senderNickname(chatMessage.getNickname())
                        .build()
                )
                .collect(Collectors.toList());
        return response;
    }
}
