package stack.moaticket.application.component.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket_alarm.dto.TicketAlarmDto;
import stack.moaticket.domain.ticket_alarm.service.TicketAlarmService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TicketReleaseHandler {
    private final AlarmService alarmService;
    private final TicketAlarmService ticketAlarmService;

    public Long handle(List<Long> ticketIdList, Map<Long, TicketMetaDto> meta, Long cursor, int limit) {
        List<TicketAlarmDto> dtoList = ticketAlarmService.getDtoList(ticketIdList, cursor, limit);
        if(dtoList.isEmpty()) return null;
        Long nextCursor = dtoList.size() < limit ? null : dtoList.getLast().id();

        Map<Long, List<Long>> receiverMap = new HashMap<>();

        for(TicketAlarmDto dto : dtoList) {
            Long memberId = dto.memberId();
            Long ticketId = dto.ticketId();

            receiverMap.computeIfAbsent(memberId, k -> new ArrayList<>())
                    .add(ticketId);
        }

        alarmService.sendTicketReleaseInform(receiverMap, meta);

        return nextCursor;
    }
}
