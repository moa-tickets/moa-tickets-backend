package stack.moaticket.application.component.registry;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class StompRoomRegistry {

    // roomId -> (userId -> sessionId)
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, SessionInfo>> roomUserSessionMap = new ConcurrentHashMap<>();

    // sessionId -> roomId
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    // sessionId -> userId
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /* ================= 등록 ================= */

    public String register(Long userId, String sessionId, String roomId) {

        ConcurrentHashMap<Long, SessionInfo> userSessionMap =
                roomUserSessionMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

        SessionInfo newInfo = new SessionInfo(sessionId);

        SessionInfo oldInfo = userSessionMap.put(userId, newInfo);

        sessionRoomMap.put(sessionId, roomId);
        sessionUserMap.put(sessionId, userId);

        return oldInfo != null ? oldInfo.getSessionId() : null; // 없으면 null
    }

    /* ================= 해제 ================= */

    public void unregisterBySession(String sessionId) {

        String roomId = sessionRoomMap.remove(sessionId);
        Long userId = sessionUserMap.remove(sessionId);

        if (roomId == null || userId == null) return;

        ConcurrentHashMap<Long, SessionInfo> userSessionMap = roomUserSessionMap.get(roomId);
        if (userSessionMap != null) {
            userSessionMap.remove(userId);

            if (userSessionMap.isEmpty()) {
                roomUserSessionMap.remove(roomId);
            }
        }
    }

    /* ================= 조회 ================= */

    public int roomSize(String roomId) {
        return roomUserSessionMap.getOrDefault(roomId, new ConcurrentHashMap<>()).size();
    }

    /* ================= lastSeen 갱신 ================= */

    public void touch(String sessionId) {
        String roomId = sessionRoomMap.get(sessionId);
        Long userId = sessionUserMap.get(sessionId);

        if (roomId == null || userId == null) return;

        ConcurrentHashMap<Long, SessionInfo> map = roomUserSessionMap.get(roomId);
        if (map == null) return;

        SessionInfo info = map.get(userId);
        if (info != null) {
            info.touch();
        }
    }
}
