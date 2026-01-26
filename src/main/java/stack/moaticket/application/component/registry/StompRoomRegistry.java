package stack.moaticket.application.component.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

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
                throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
            }
        }
    }

    /* ================= 등록 ================= */

    public String register(Long memberId, String sessionId, String roomId) {

        ConcurrentHashMap<Long, SessionInfo> userSessionMap =
                roomMemberSessionMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

        SessionInfo newInfo = new SessionInfo(sessionId);

        SessionInfo oldInfo = userSessionMap.put(memberId, newInfo);


        if (oldInfo != null) {
            sessionMemberMap.remove(oldInfo.getSessionId());
            sessionRoomMap.remove(oldInfo.getSessionId());
        }
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

            userSessionMap.computeIfPresent(memberId, (mId, info) -> {
                return sessionId.equals(info.getSessionId()) ? null : info;
            });

            return userSessionMap.isEmpty() ? null : userSessionMap;
        });
    }

    /* ================= 조회 ================= */

    public int roomSize(String roomId) {
        ConcurrentHashMap<Long, SessionInfo> room = roomMemberSessionMap.get(roomId);
        return room != null ? room.size() : 0;
    }
    public int sessionRoomMapSize() {
        return sessionRoomMap.size();
    }
    public int sessionMemberMapSize() {
        return sessionMemberMap.size();
    }
    public int wsSessionMapSize() {
        return wsSessionMap.size();
    }

    /* ================= lastSeen 갱신 ================= */

    public void touch(String sessionId) {
        String roomId = sessionRoomMap.get(sessionId);
        Long userId = sessionMemberMap.get(sessionId);

        if (roomId == null || userId == null) throw new MoaException(MoaExceptionType.NOT_FOUND);

        ConcurrentHashMap<Long, SessionInfo> map = roomMemberSessionMap.get(roomId);
        if (map == null) throw new MoaException(MoaExceptionType.NOT_FOUND);

        SessionInfo info = map.get(userId);
        if (info != null) {
            info.touch();
        }
    }
}
