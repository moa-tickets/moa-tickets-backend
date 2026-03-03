package stack.moaticket.system.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @DisplayName("jwtUtil jwt생성 및 getSubject 테스트")
    @Test
    void createJwtAndGetSubject(){
        // given
        Long memberId = 23L;
        JwtUtil jwtUtil = new JwtUtil("test-secret-test-secret-test-secret-test-secret");
        // when
        String token = jwtUtil.createJwt(memberId, 100000L);
        // then
        assertThat(memberId).isEqualTo(jwtUtil.getSubject(token));
    }


}