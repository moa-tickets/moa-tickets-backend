package stack.moaticket.application.component.gauge;

import lombok.Getter;

@Getter
public final class TicketReleaseMarks  {
    private boolean locked = false;
    private boolean unlocked = false;
    private boolean done = false;
    private boolean acked = false;

    public void markLocked() { locked = true; }
    public void markUnlocked() { unlocked = true; }
    public void markDone() { done = true; }
    public void markAcked() { acked = true; }
}
