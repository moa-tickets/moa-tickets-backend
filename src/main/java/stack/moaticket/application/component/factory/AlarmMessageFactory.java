package stack.moaticket.application.component.factory;

import org.springframework.stereotype.Component;
import stack.moaticket.application.port.AlarmMessage;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;

import java.util.List;

@Component
public class AlarmMessageFactory {
    public AlarmMessage sessionStart(SessionStartAlarmMetaDto alarm) {
        String key = switch (alarm.type()) {
            case LEFT_10 -> "SS_LEFT_10";
            case ON_HOUR -> "SS_ON_HOUR";
        };
        return new AlarmMessage(key, alarm);
    }

    public AlarmMessage ticketRelease(List<TicketMetaDto> metaList) {
        String key = (metaList.size() > 1) ? "TR_BULK" : "TR_SINGLE";
        return new AlarmMessage(key, metaList);
    }
}
