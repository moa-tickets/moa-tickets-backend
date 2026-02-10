package stack.moaticket.system.alarm.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlarmShardUtil {
    private AlarmShardUtil() {}

    public static int getShardNum(Long memberId, int shardCount) {
        return Math.floorMod(memberId, shardCount);
    }

    public static <T> Map<Integer, List<T>> createShardMap(int shardCount) {
        Map<Integer, List<T>> shardMap = new HashMap<>();
        for(int i = 0; i < shardCount; i++) {
            shardMap.put(i, new ArrayList<>());
        }
        return shardMap;
    }
}
