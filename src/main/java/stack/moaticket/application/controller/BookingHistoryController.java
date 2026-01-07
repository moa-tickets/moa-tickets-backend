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

    // 예매내역 목록 조회 (10개 offset pagination)
    // - 기간필터: range=D15|M1|M2|M3
    // - 월별필터: basis=BOOKED_AT|VIEWED_AT & year=YYYY & month=1~12
    @GetMapping("/me")
    public ResponseEntity<BookingHistoryDto.ListResponse> getList(
            @AuthenticationPrincipal Member member,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) BookingHistoryDto.RangeFilter range,
            @RequestParam(required = false) BookingHistoryDto.MonthBasis basis,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(
                bookingHistoryService.getList(member.getId(), page, range, basis, year, month)
        );
    }
}
