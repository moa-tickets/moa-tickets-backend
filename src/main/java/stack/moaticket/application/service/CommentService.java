package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.CommentDto;
import stack.moaticket.domain.comment.entity.Comment;
import stack.moaticket.domain.comment.repository.CommentRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService {

    private final Validator validator;
    private final MemberService memberService;
    private final CommentRepository commentRepository;

    public void create(Long memberId, CommentDto.Request commentRequest) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Comment comment = requestToEntity(commentRequest);
        commentRepository.save(comment);
    }

    public void fix(Long memberId, CommentDto.CommentFixRequest commentFixRequest) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Comment commentEntity = commentRepository.findById(commentFixRequest.commentId()).get();
        commentEntity.fix(commentFixRequest);
        commentRepository.save(commentEntity);

    }

    public Comment requestToEntity(CommentDto.Request request) {
        return Comment.builder()
                .nickName(request.getNickName())
                .content(request.getContent())
                .build();
    }
}
