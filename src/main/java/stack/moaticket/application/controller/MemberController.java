package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.application.dto.GetMemberDto;
import stack.moaticket.application.service.MemberInfoService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberInfoService memberInfoService;

    @GetMapping("/api/members/me")
    public ResponseEntity<GetMemberDto.Response> getMember(
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.NOT_FOUND);
        }
        return ResponseEntity.ok(memberInfoService.getMember(member));
    }
}
