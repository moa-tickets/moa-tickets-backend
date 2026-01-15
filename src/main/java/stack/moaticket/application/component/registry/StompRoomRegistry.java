package stack.moaticket.application.component.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class StompRoomRegistry {

    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, SessionInfo>> roomMemberSessionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionMemberMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, WebSocketSession> wsSessionMap = new ConcurrentHashMap<>();

    /* ================= WebSocketSession 등록/해제 ================= */

    public void registerWsSession(WebSocketSession session) {
        wsSessionMap.put(session.getId(), session);
    }

    public void unregisterWsSession(String sessionId) {
        wsSessionMap.remove(sessionId);
    }

    public void closeSession(String sessionId, CloseStatus status) {
        WebSocketSession ws = wsSessionMap.get(sessionId);
        if (ws != null && ws.isOpen()) {
            try {
                ws.close(status);
            } catch (Exception e) {
                log.error("웹소켓 세션 종료중 오류 발생 . sessionId : {}, status : {}, {}", sessionId, status, e);
            }
        }
    }

    /* ================= 등록 ================= */

    public String register(Long memberId, String sessionId, String roomId) {

        ConcurrentHashMap<Long, SessionInfo> userSessionMap =
                roomMemberSessionMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

        SessionInfo newInfo = new SessionInfo(sessionId);

        SessionInfo oldInfo = userSessionMap.put(memberId, newInfo);

        sessionRoomMap.put(sessionId, roomId);
        sessionMemberMap.put(sessionId, memberId);

        return oldInfo != null ? oldInfo.getSessionId() : null; // 없으면 null
    }

    /* ================= 해제 ================= */

    public void unregisterBySession(String sessionId) {

        String roomId = sessionRoomMap.remove(sessionId);
        Long memberId = sessionMemberMap.remove(sessionId);

        if (roomId == null || memberId == null) return;

        roomMemberSessionMap.computeIfPresent(roomId, (rId, userSessionMap) -> {
            userSessionMap.remove(memberId);
            if (userSessionMap.isEmpty()) {
                return null; // 맵이 비었으면 roomMemberSessionMap에서 해당 roomId 항목을 제거
            }
            return userSessionMap;
        });
    }

    /* ================= 조회 ================= */

    public int roomSize(String roomId) {
        ConcurrentHashMap<Long, SessionInfo> room = roomMemberSessionMap.get(roomId);
        return room != null ? room.size() : 0;
    }

    /* ================= lastSeen 갱신 ================= */

    public void touch(String sessionId) {
        String roomId = sessionRoomMap.get(sessionId);
        Long userId = sessionMemberMap.get(sessionId);

        if (roomId == null || userId == null) return;

        ConcurrentHashMap<Long, SessionInfo> map = roomMemberSessionMap.get(roomId);
        if (map == null) return;

        SessionInfo info = map.get(userId);
        if (info != null) {
            info.touch();
        }
    }
}
