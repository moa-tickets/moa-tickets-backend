package stack.moaticket.test.service;

import lombok.RequiredArgsConstructor;
import stack.moaticket.system.jwt.JwtUtil;

@RequiredArgsConstructor
public class LoadTestAuthService {
    private final JwtUtil jwtUtil;

    public String issueToken(long memberId) {
        long expiredMs = 24 * 60 * 60 * 1000L;
        return jwtUtil.createJwt(memberId, expiredMs);
    }
}
