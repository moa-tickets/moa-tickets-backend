package stack.moaticket.test.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BookingDto;
import stack.moaticket.application.service.BookingService;

@ConditionalOnProperty(
        value = "app.server.test.load-test.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RestController
@RequestMapping("/test/booking")
@RequiredArgsConstructor
public class BookingTestController {
    private final BookingService bookingService;

    @PostMapping("/tickets/hold")
    public ResponseEntity<BookingDto.HoldResponse> holdTickets(
            @RequestHeader("X-LoadTest-Id") String memberId,
            @RequestBody BookingDto.HoldRequest request) {
        BookingService.HoldResult result =
                bookingService.holdTickets(Long.parseLong(memberId), request.getSessionId(), request.getTicketIds());

        BookingDto.HoldResponse response = BookingDto.HoldResponse.builder()
                .holdToken(result.holdToken())
                .expiresAt(result.expiresAt())
                .build();

        return ResponseEntity.ok(response);
    }
}