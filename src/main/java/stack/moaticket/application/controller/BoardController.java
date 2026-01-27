package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BoardDto;
import stack.moaticket.application.service.BoardService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BoardController {

    private final BoardService boardService;

    @PostMapping("/create")
    public void createBoard(@RequestBody BoardDto.Request request) {
        boardService.create(request);
    }

    //전체 조회
    @GetMapping("/board")
    public List<BoardDto.BoardResponse> readBoards() {
        return boardService.reads();
    }

    //하나만 조회
    @GetMapping("/board/{id}")
    public BoardDto.BoardResponse read(
            @PathVariable Long id
    ) {
        return boardService.read(id);
    }

    @PatchMapping("/board/patch")
    public void fix(@RequestBody BoardDto.BoardFixRequest boardFixRequest) {
        boardService.fix(boardFixRequest);
    }

    @DeleteMapping("/board/delete/{id}")
    public void delete(@PathVariable Long id) {
        boardService.delete(id);
    }
}