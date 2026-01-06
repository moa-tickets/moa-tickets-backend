package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.application.dto.GetMemberDto;
import stack.moaticket.application.service.MemberService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.health.ExceptionTestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/member")
    public ResponseEntity<GetMemberDto.Response> getMember(
            @AuthenticationPrincipal Member member
    ) {
        return ResponseEntity.ok(memberService.getMember(member));
    }
}
