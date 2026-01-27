package stack.moaticket.application.service;

import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.BoardDto;
import stack.moaticket.domain.board.entity.Board;

import java.util.List;

@Service
public interface BoardService {

    void create(Long memberId, BoardDto.Request request);

    BoardDto.BoardResponse read(Long id);

    List<BoardDto.BoardResponse> reads();

    void fix(Long memberId, BoardDto.BoardFixRequest boardFixRequest);

    void delete(Long memberId, Long id);

    default Board requestToEntity(BoardDto.Request request) {
        return Board.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .nickName(request.getNickName())
                .build();

    }

    //엔티티로 만들어서 전체조회
    default BoardDto.BoardResponse entityToResponse(Board board) {
        return BoardDto.BoardResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .nickName(board.getNickName())
                .build();
    }

}
