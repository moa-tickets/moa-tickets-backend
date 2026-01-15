package stack.moaticket.application.component.registry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SessionInfo {

    private final String sessionId;
    private volatile long lastSeen;

    public void touch() {
        this.lastSeen = System.currentTimeMillis();
    }
}
