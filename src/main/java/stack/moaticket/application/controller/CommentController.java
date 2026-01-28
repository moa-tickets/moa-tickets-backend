package stack.moaticket.application.controller;

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

    @PostMapping("/comment")
    public void createComment(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CommentDto.Request request) {
        commentService.create(memberId, request);

    }

    @PatchMapping("/comment/{commentId}")
    public void fix(@AuthenticationPrincipal Long memberId,
                    @RequestBody CommentDto.CommentFixRequest commentFixRequest,
                    @PathVariable Long commentId) {
        commentService.fix(memberId, commentFixRequest, commentId);

    }

    @DeleteMapping("/comment/delete/{id}")
    public void delete(@AuthenticationPrincipal Long memberId, @PathVariable Long id) {
        commentService.delete(memberId, id);
    }
}

