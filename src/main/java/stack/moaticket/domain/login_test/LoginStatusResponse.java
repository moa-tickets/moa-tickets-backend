package stack.moaticket.domain.login_test;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginStatusResponse {
    private boolean loggedIn;

    public static LoginStatusResponse of(boolean loggedIn) {
        return new LoginStatusResponse(loggedIn);
    }
}
