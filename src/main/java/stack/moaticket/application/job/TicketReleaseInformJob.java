package stack.moaticket.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stack.moaticket.application.component.scheduler.JobSchedulerProperties;
import stack.moaticket.application.facade.HoldCleanedInformFacade;
import stack.moaticket.application.service.AlarmService;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket_alarm.dto.TicketAlarmDto;
import stack.moaticket.domain.ticket_alarm.service.TicketAlarmService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketReleaseInformJob {
    private final AlarmService alarmService;
    private final TicketAlarmService ticketAlarmService;
    private final HoldCleanedInformFacade holdCleanedInformFacade;
    private final JobSchedulerProperties properties;

    private final Long batchSize = properties.ticketRelease().batchSize();
    private final int loopCount = properties.ticketRelease().loopCount();
    private final int pageLimit = properties.ticketRelease().pageLimit();

    public void runEpoch() {
        int retry = loopCount;

        while(retry-- > 0) {
            LocalDateTime now = LocalDateTime.now();

            List<Long> ticketIdList = holdCleanedInformFacade.extractCandidates(now, batchSize);
            if(ticketIdList.isEmpty()) break;

            holdCleanedInformFacade.release(now, ticketIdList);
            Map<Long, TicketMetaDto> ticketMetadata = holdCleanedInformFacade.getChanged(ticketIdList)
                    .stream()
                    .collect(Collectors.toMap(
                            TicketMetaDto::ticketId,
                            Function.identity()
                    ));
            if(ticketMetadata.isEmpty()) break;

            sendByPages(ticketIdList, ticketMetadata, pageLimit);
        }
    }

    private void sendByPages(List<Long> ticketIdList, Map<Long, TicketMetaDto> ticketMetadata, int limit) {
        Long cursor = null;

        while(true) {
            List<TicketAlarmDto> dtoList = ticketAlarmService.getDtoList(ticketIdList, cursor, limit);
            if(dtoList.isEmpty()) break;
            cursor = dtoList.getLast().id();

            Map<Long, List<Long>> receiverMap = new HashMap<>();

            for(TicketAlarmDto dto : dtoList) {
                Long memberId = dto.memberId();
                Long ticketId = dto.ticketId();

                receiverMap.computeIfAbsent(memberId, k -> new ArrayList<>())
                        .add(ticketId);
            }

            alarmService.sendTicketReleaseInform(receiverMap, ticketMetadata);
        }
    }
}
