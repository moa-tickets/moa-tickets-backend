package stack.moaticket.system.alarm.core.util;

import stack.moaticket.system.alarm.core.model.*;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;

import java.util.List;

public class AlarmMessageFactory {
    public static AlarmMessage<ConnectPayload> connect(String connectionId) {
        ConnectPayload payload = new ConnectPayload(connectionId);
        return new AlarmMessage<>("CONNECT", payload);
    }

    public static AlarmMessage<HeartbeatPayload> heartbeat() {
        HeartbeatPayload payload = new HeartbeatPayload("ping");
        return new AlarmMessage<>("HEARTBEAT", payload);
    }

    public static AlarmMessage<SessionStartPayload> sessionStart(SessionStartAlarmMetaDto alarm) {
        SessionStartPayload payload = new SessionStartPayload(alarm);
        String key = switch (alarm.type()) {
            case LEFT_10 -> "SS_LEFT_10";
            case ON_HOUR -> "SS_ON_HOUR";
        };
        return new AlarmMessage<>(key, payload);
    }

    public static AlarmMessage<TicketReleasePayload> ticketRelease(List<TicketMetaDto> metaList) {
        TicketReleasePayload payload = new TicketReleasePayload(metaList);

        String key = (metaList.size() > 1) ? "TR_BULK" : "TR_SINGLE";
        return new AlarmMessage<>(key, payload);
    }
}
