package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BoardDto;
import stack.moaticket.application.service.BoardService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BoardController {

    private final BoardService boardService;

    @PostMapping("/board")
    public void createBoard(
            @AuthenticationPrincipal Long memberId,
            @RequestBody BoardDto.Request request) {
        boardService.create(memberId, request);
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
    public void fix(@AuthenticationPrincipal Long memberId, @RequestBody BoardDto.BoardFixRequest boardFixRequest) {
        boardService.fix(memberId, boardFixRequest);
    }

    @DeleteMapping("/board/delete/{id}")
    public void delete(@AuthenticationPrincipal Long memberId, @PathVariable Long id) {
        boardService.delete(memberId, id);
    }
}