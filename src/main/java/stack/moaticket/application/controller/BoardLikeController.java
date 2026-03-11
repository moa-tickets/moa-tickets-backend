package stack.moaticket.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BoardLikeDto;
import stack.moaticket.application.service.BoardLikeService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BoardLikeController {

    private final BoardLikeService boardLikeService;

//     내 좋아요 목록
    @GetMapping("/likes")
    public List<BoardLikeDto.BoardLikeResponse> readLikes(
            @AuthenticationPrincipal Long memberId
    ) {
        return boardLikeService.readLikes(memberId);
    }

    @PostMapping("/boards/{boardId}/likes")
    public BoardLikeDto.BoardLikeActionResponse createLike(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long boardId
    ) {
        return boardLikeService.createLike(memberId, boardId);
    }

    //좋아요 row삭제
    @DeleteMapping("/boards/{boardId}/likes")
    public BoardLikeDto.BoardLikeActionResponse deleteLike(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long boardId
    ) {
        return boardLikeService.deleteLike(memberId, boardId);
    }
}
