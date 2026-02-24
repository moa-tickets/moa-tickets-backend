package stack.moaticket.application.service;

import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.BoardDto;
import stack.moaticket.domain.board.entity.Board;
import stack.moaticket.domain.member.entity.Member;

import java.util.List;

@Service
public interface BoardService {

    void create(Long memberId, BoardDto.BoardPostRequest boardPostRequest);

    BoardDto.BoardResponse read(Long id);

    List<BoardDto.BoardResponse> reads();

    void fix(Long memberId, BoardDto.BoardFixRequest boardFixRequest, Long boardId);

    void delete(Long memberId, Long id);

    default Board requestToEntity(Member member, BoardDto.BoardPostRequest boardPostRequest) {

        return Board.builder()
                .title(boardPostRequest.getTitle())
                .content(boardPostRequest.getContent())
                .member(member)
                .build();
    }

    //엔티티로 만들어서 전체조회
    default BoardDto.BoardResponse entityToResponse(Board board) {
        return BoardDto.BoardResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .nickName(board.getMember().getNickname())
                .likeCount(board.getLikeCount())
                .createdAt(board.getCreatedAt())
                .build();
    }
}