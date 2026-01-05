package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BookingDto;
import stack.moaticket.application.service.BookingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    /**
     * 회차별 좌석 목록 조회 (좌석 배치도용)
     *
     * 정책:
     * - HOLD 상태라도 holdExpired가 지난 좌석은 AVAILABLE로 내려준다
     *   (DB를 즉시 업데이트하지 않아도 "보이는 상태"는 AVAILABLE)
     */
    @GetMapping("/sessions/{sessionId}/tickets")
    public ResponseEntity<List<BookingDto.TicketResponse>> getTicketsBySession(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(bookingService.getTicketsBySession(sessionId));
    }

    /**
     * 좌석 임시 점유 (HOLD)
     *
     * Request: { sessionId, ticketIds(최대 4개) }
     * Response: { holdToken, expiresAt }
     */
    @PostMapping("/tickets/hold")
    public ResponseEntity<BookingDto.HoldResponse> holdTickets(
            @AuthenticationPrincipal Long memberId,
            @RequestBody BookingDto.HoldRequest request
    ) {
        BookingService.HoldResult result =
                bookingService.holdTickets(memberId, request.getSessionId(), request.getTicketIds());

        BookingDto.HoldResponse response = BookingDto.HoldResponse.builder()
                .holdToken(result.holdToken())
                .expiresAt(result.expiresAt())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 점유 확정 (HOLD -> SOLD)
     */
    @PostMapping("/holds/{holdToken}/confirm")
    public ResponseEntity<Void> confirmHold(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String holdToken
    ) {
        bookingService.confirmHold(memberId, holdToken);
        return ResponseEntity.ok().build();
    }

    /**
     * 점유 해제 (HOLD -> AVAILABLE)
     * - 정책: 이미 만료되어 AVAILABLE 상태여도 성공(200)으로 처리
     */
    @PostMapping("/holds/{holdToken}/release")
    public ResponseEntity<Void> releaseHold(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String holdToken
    ) {
        bookingService.releaseHold(memberId, holdToken);
        return ResponseEntity.ok().build();
    }

}
