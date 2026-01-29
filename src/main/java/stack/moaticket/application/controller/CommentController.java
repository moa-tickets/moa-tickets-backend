package stack.moaticket.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.CommentDto;
import stack.moaticket.application.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/board/{boardId}/comments")
    public void createComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long boardId,
            @Valid @RequestBody CommentDto.Request request) {
        commentService.create(memberId, boardId, request);

    }

    @PatchMapping("/comment/{commentId}")
    public void fix(@AuthenticationPrincipal Long memberId,
                    @RequestBody CommentDto.CommentFixRequest commentFixRequest,
                    @PathVariable Long commentId) {
        commentService.fix(memberId, commentFixRequest, commentId);

    }

    @DeleteMapping("/comment/{commentId}")
    public void delete(@AuthenticationPrincipal Long memberId, @PathVariable Long commentId) {
        commentService.delete(memberId, commentId);
    }
}

