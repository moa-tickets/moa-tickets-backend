package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.CommentDto;
import stack.moaticket.domain.board.entity.Board;
import stack.moaticket.domain.board.repository.BoardRepository;
import stack.moaticket.domain.comment.entity.Comment;
import stack.moaticket.domain.comment.repository.CommentRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService {

    private final Validator validator;
    private final MemberService memberService;
    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;

    public void create(Long memberId, Long boardId, CommentDto.Request commentRequest) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.ENTITY_NOT_FOUND,
                        "해당 게시글을 찾을 수 없습니다. id=" + boardId));

        Comment comment = requestToEntity(board, commentRequest);
        commentRepository.save(comment);
    }

    public void fix(Long memberId, CommentDto.CommentFixRequest commentFixRequest, Long commentId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Comment commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.ENTITY_NOT_FOUND));

        if (!commentEntity.getCommenter().equals(member)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        commentEntity.fix(commentFixRequest);
        commentRepository.save(commentEntity);


    }

    public void delete(Long memberId, Long commentId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Comment commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.ENTITY_NOT_FOUND));

        if (!commentEntity.getCommenter().equals(member)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        commentRepository.deleteById(commentId);
    }


    public Comment requestToEntity(Board board, CommentDto.Request request) {
        return Comment.builder()
                .board(board)
                .content(request.getContent())
                .build();
    }
}