package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.BoardLikeDto;
import stack.moaticket.domain.board.entity.Board;
import stack.moaticket.domain.board.repository.BoardRepository;
import stack.moaticket.domain.board_like.entity.BoardLike;
import stack.moaticket.domain.board_like.repository.BoardLikeRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class BoardLikeService {

    private final Validator validator;
    private final BoardLikeRepository boardLikeRepository;
    private final MemberService memberService;
    private final BoardRepository boardRepository;

    @Transactional(readOnly = true)
    public List<BoardLikeDto.BoardLikeResponse> readLikes(Long memberId) {

        List<BoardLike> likes = boardLikeRepository.findByMemberId(memberId);
        List<BoardLikeDto.BoardLikeResponse> responses = new ArrayList<>();

        for (BoardLike boardLike : likes) {
            Board board = boardLike.getBoard();

            BoardLikeDto.BoardLikeResponse response = BoardLikeDto.BoardLikeResponse.builder()
                    .boardId(boardLike.getBoard().getId())
                    .likeCount(board.getLikeCount())
                    .createdAt(boardLike.getCreatedAt())
                    .build();

            responses.add(response);
        }
        return responses;
    }

    @Transactional
    public BoardLikeDto.BoardLikeActionResponse createLike(Long memberId, Long boardId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.ENTITY_NOT_FOUND));

        boolean exists = boardLikeRepository.existsByBoardIdAndMemberId(boardId, memberId);
        if (!exists) {
            BoardLike boardLike = BoardLike.builder()
                    .member(member)
                    .board(board)
                    .build();
            boardLikeRepository.save(boardLike);

            board.increaseLikeCount();
        }

        return BoardLikeDto.BoardLikeActionResponse.builder()
                .boardId(boardId)
                .myLiked(true)
                .likeCount(board.getLikeCount())
                .build();
    }

    @Transactional
    public BoardLikeDto.BoardLikeActionResponse deleteLike(Long memberId, Long boardId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.ENTITY_NOT_FOUND));

        boolean exists = boardLikeRepository.existsByBoardIdAndMemberId(boardId, memberId);
        if (exists){
            boardLikeRepository.deleteByBoardIdAndMemberId(boardId, memberId);

            board.decreaseLikeCount();
        }

        return BoardLikeDto.BoardLikeActionResponse.builder()
                .boardId(boardId)
                .myLiked(false)
                .likeCount(board.getLikeCount())
                .build();
    }
}