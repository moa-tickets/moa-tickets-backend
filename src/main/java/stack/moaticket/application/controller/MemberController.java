package stack.moaticket.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.application.dto.GetMemberDto;
import stack.moaticket.application.service.MemberInfoService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;

@Tag(name = "Member API", description = "유저 정보 조회 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberInfoService memberInfoService;
    private final MemberService memberService;

    @Operation(
            summary = "로그인 유저 정보 조회",
            description = "로그인 된 유저 정보를 조회 함",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GetMemberDto.Response.class)
                            )
                    )
            }

    )
    @GetMapping("/api/members/me")
    public ResponseEntity<GetMemberDto.Response> getMember(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberInfoService.getMember(memberId));
    }

    @Operation(
            summary = "테스트용 seller",
            description = "로그인 된 유저를 seller로 변경"
    )
    @PostMapping("/api/members/seller")
    public ResponseEntity<Member> convertToSeller(
            @AuthenticationPrincipal Long memberId) {
        Member updatedMember = memberService.convertToSeller(memberId);
        return ResponseEntity.ok(updatedMember);
    }

    @Operation(
            summary = "테스트용 buyer",
            description = "로그인 된 유저를 buyer로 변경"
    )
    @PostMapping("/api/members/buyer")
    public ResponseEntity<Member> convertToBuyer(
            @AuthenticationPrincipal Long memberId) {
        Member updatedMember = memberService.convertToBuyer(memberId);
        return ResponseEntity.ok(updatedMember);
    }
}
