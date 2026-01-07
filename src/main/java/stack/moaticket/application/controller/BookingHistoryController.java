package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BookingHistoryDto;
import stack.moaticket.application.service.BookingHistoryService;
import stack.moaticket.domain.member.entity.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingHistoryController {

    private final BookingHistoryService bookingHistoryService;

    // 예매내역 상세 조회 (PAID + CANCELED)
    @GetMapping("/me/{orderId}")
    public ResponseEntity<BookingHistoryDto.DetailResponse> getDetail(
            @AuthenticationPrincipal Member member,
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(
                bookingHistoryService.getDetail(member.getId(), orderId)
        );
    }
}
