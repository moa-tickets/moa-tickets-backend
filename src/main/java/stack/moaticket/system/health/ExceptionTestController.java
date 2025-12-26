package stack.moaticket.system.health;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/test")
public class ExceptionTestController {

    @GetMapping("/exception")
    public String testException() {
        throw new RuntimeException("테스트 예외입니다. 이 메시지는 dev 환경에서만 보여야 합니다.");
    }
}
