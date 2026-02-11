package stack.moaticket.test.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.test.service.LoadTestAuthService;

import java.util.Map;

@RestController
@RequestMapping("/test/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "app.server.test.load-test.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class LoadTestAuthController {

    private final LoadTestAuthService loadTestAuthService;

    @GetMapping("/token")
    public Map<String, String> token(@RequestParam long memberId) {
        String token = loadTestAuthService.issueToken(memberId);
        return Map.of("token", token);
    }
}
