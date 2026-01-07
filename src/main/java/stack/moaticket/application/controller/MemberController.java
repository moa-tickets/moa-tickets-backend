package stack.moaticket.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Member API", description = "유저 정보 조회 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberInfoService memberInfoService;

    @Operation(
            summary = "로그인 유저 정보 조회",
            description = "로그인 된 유저 정보를 조회 함",
            parameters = {
                    @Parameter(
                            name = "member",
                            description = "로그인된 유저",
                            required = true
                    )
            },
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
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        return ResponseEntity.ok(memberInfoService.getMember(member));
    }
}
