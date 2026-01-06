package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.application.dto.GetMemberDto;
import stack.moaticket.application.service.MemberInfoService;
import stack.moaticket.domain.member.entity.Member;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberInfoService memberInfoService;

    @GetMapping("/member")
    public ResponseEntity<GetMemberDto.Response> getMember(
            @AuthenticationPrincipal Member member
    ) {
        return ResponseEntity.ok(memberInfoService.getMember(member));
    }
}
