package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.domain.chat_message.entity.ChatMessage;
import stack.moaticket.domain.chat_message.repository.ChatMessageRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

@Service
@RequiredArgsConstructor
public class ChattingService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepositoryQueryDsl memberRepositoryQueryDsl;
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

        ChattingDto response = ChattingDto.builder()
                .message(chatMessage.getContent())
                .senderNickname(member.getNickname())
                .build();

        messagingTemplate.convertAndSend("/sub/" + playbackId + "/messages", response);
    }
}
