package stack.moaticket.domain.hall.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HallType {
    SMALL(8, 10), MEDIUM(14, 20), LARGE(20, 40);

    private final int row;
    private final int col;

    public int total() {
        return row * col;
    }
}
