package stack.moaticket.system.alarm.core.component;

import org.springframework.stereotype.Component;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.List;

@Component
public class AlarmMessageFactory {
    public static AlarmMessage connect() {
        return new AlarmMessage("CONNECT", "connected");
    }

    public static AlarmMessage heartbeat() {
        return new AlarmMessage("HEARTBEAT", "ping");
    }

    public AlarmMessage sessionStart(SessionStartAlarmMetaDto alarm) {
        String key = switch (alarm.type()) {
            case LEFT_10 -> "SS_LEFT_10";
            case ON_HOUR -> "SS_ON_HOUR";
        };
        return new AlarmMessage(key, alarm);
    }

    public AlarmMessage ticketRelease(List<TicketMetaDto> metaList) {
        if(metaList == null || metaList.isEmpty()) throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);

        String key = (metaList.size() > 1) ? "TR_BULK" : "TR_SINGLE";
        return new AlarmMessage(key, metaList);
    }
}
