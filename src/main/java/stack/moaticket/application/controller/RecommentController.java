package stack.moaticket.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.RecommentDto;
import stack.moaticket.application.service.RecommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RecommentController {
    private final RecommentService recommentService;

    @GetMapping("/recomments")
    public List<RecommentDto.RecommentResponse> readRecomments() {
        return recommentService.reads();
    }

    @GetMapping("/recomments/{id}")
    public RecommentDto.RecommentResponse read(
            @PathVariable Long id
    ) {
        return recommentService.read(id);
    }

    @PostMapping("/comments/{commentId}/recomment")
    public void createRecomment(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RecommentDto.Request request,
            @PathVariable Long commentId) {
        recommentService.create(memberId, request, commentId);
    }

    @PatchMapping("/recomments/{recommendId}")
    public void fixRecomment(@AuthenticationPrincipal Long memberId,
                             @Valid @RequestBody RecommentDto.RecommentFixRequest recommentFixRequest,
                             @PathVariable Long recommendId) {
        recommentService.fix(memberId, recommentFixRequest, recommendId);
    }

    @DeleteMapping("/recomments/{recommentId}")
    public void deleteRecomment(@AuthenticationPrincipal Long memberId,
                                @PathVariable Long recommentId) {
        recommentService.delete(memberId, recommentId);
    }
}
