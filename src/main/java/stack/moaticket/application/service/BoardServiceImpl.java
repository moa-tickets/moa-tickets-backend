package stack.moaticket.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.BoardDto;
import stack.moaticket.domain.board.entity.Board;
import stack.moaticket.domain.board.repository.BoardRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;

    @Override
    public void create(BoardDto.Request boardRequest) {
        Board board = requestToEntity(boardRequest);

        boardRepository.save(board);
    }

    @Override
    public BoardDto.BoardResponse read(Long id) {
        Board boardEntity = boardRepository.findById(id).get();
        return entityToResponse(boardEntity);
    }

    @Override
    public List<BoardDto.BoardResponse> reads() {
        List<Board> boardList = boardRepository.findAll();
        List<BoardDto.BoardResponse> res = new ArrayList<>();

        for (Board b : boardList) {
            res.add(this.entityToResponse(b));
        }
        return res;
    }

    @Override
    public void fix(BoardDto.BoardFixRequest boardFixRequest) {
        Board boardEntity = boardRepository.findById(boardFixRequest.boardId()).get();
        boardEntity.fix(boardFixRequest);
        //가독성을 위해 save을 한 번더 작성
        boardRepository.save(boardEntity);
    }

    @Override
    public void delete(Long id) {
        boardRepository.deleteById(id);
    }
}
