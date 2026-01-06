package stack.moaticket.system.health;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.domain.login_test.LoginStatusResponse;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Profile("dev")
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class ExceptionTestController {

    private final MemberRepositoryQueryDsl memberRepositoryQueryDsl;
  
    @GetMapping("/exception")
    public String testException() {
        throw new RuntimeException("테스트 예외입니다. 이 메시지는 dev 환경에서만 보여야 합니다.");
    }

    @GetMapping("/api/auth/status")
    public LoginStatusResponse status(Authentication authentication) {
        return LoginStatusResponse.of(authentication != null);
    }

    @GetMapping("/me")
    public ResponseEntity<MemberTestResponse> getCurrentMember(
            @AuthenticationPrincipal Member member
    ) {
        Long memberId = member.getId();
        log.info("테스트 API 호출: memberId={}", memberId);

        if (memberId == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }

        if (member == null) {
            throw new MoaException(MoaExceptionType.NOT_FOUND);
        }

        MemberTestResponse response = MemberTestResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .isSeller(member.isSeller())
                .memberState(member.getState().name())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 테스트 응답 DTO
     */
    @lombok.Getter
    @lombok.Builder
    private static class MemberTestResponse {
        private Long memberId;
        private String email;
        private String nickname;
        private Boolean isSeller;
        private String memberState;
    }
}
