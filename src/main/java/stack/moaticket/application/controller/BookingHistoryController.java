package stack.moaticket.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BookingHistoryDto;
import stack.moaticket.application.service.BookingHistoryService;

@Tag(name = "BookingHistory API", description = "마이페이지 예매 내역 조회 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingHistoryController {

    private final BookingHistoryService bookingHistoryService;

    // 예매내역 상세 조회 (PAID + CANCELED)
    @Operation(
            summary = "내 예매내역 상세 조회",
            description = "예매번호(orderId)로 내 예매내역 상세를 조회합니다. (PAID/CANCELED 조회 가능)",
            parameters = {
                    @Parameter(
                            name = "orderId",
                            description = "예매번호(orderId)",
                            required = true,
                            in = ParameterIn.PATH
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BookingHistoryDto.DetailResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "예매 내역을 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청값이 올바르지 않음"
                    )
            }
    )
    @GetMapping("/me/{orderId}")
    public ResponseEntity<BookingHistoryDto.DetailResponse> getDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(
                bookingHistoryService.getDetail(memberId, orderId)
        );
    }

    // 예매내역 목록 조회 (10개 offset pagination)
    // - 기간필터: range=D15|M1|M2|M3
    // - 월별필터: basis=BOOKED_AT|VIEWED_AT & year=YYYY & month=1~12
    @Operation(
            summary = "내 예매내역 목록 조회",
            description = """
                    내 예매내역 목록을 페이지네이션으로 조회합니다. (10개 고정)
                    - page는 0부터 시작합니다.
                    - 필터1(기간): range=D15|M1|M2|M3 (예매일=paidAt 기준)
                    - 필터2(월별): basis=BOOKED_AT|VIEWED_AT & year=YYYY & month=1~12
                    - range와 월별 필터는 동시에 사용할 수 없습니다.
                    """,
            parameters = {
                    @Parameter(
                            name = "page",
                            description = "페이지 번호 (0부터 시작, 기본값 0)",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
                    ),
                    @Parameter(
                            name = "range",
                            description = "기간 필터 (D15:15일, M1:1개월, M2:2개월, M3:3개월)",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = BookingHistoryDto.RangeFilter.class)
                    ),
                    @Parameter(
                            name = "basis",
                            description = "월별 조회 기준 (BOOKED_AT:예매일(paidAt), VIEWED_AT:관람일(sessionDate))",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = BookingHistoryDto.MonthBasis.class)
                    ),
                    @Parameter(
                            name = "year",
                            description = "월별 조회 연도 (basis와 month와 함께 사용)",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = @Schema(type = "integer", example = "2026", minimum = "2000", maximum = "2100")
                    ),
                    @Parameter(
                            name = "month",
                            description = "월별 조회 월 (1~12, basis/year와 함께 사용)",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = @Schema(type = "integer", example = "1", minimum = "1", maximum = "12")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BookingHistoryDto.ListResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청값이 올바르지 않음"
                    )
            }
    )
    @GetMapping("/me")
    public ResponseEntity<BookingHistoryDto.ListResponse> getList(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) BookingHistoryDto.RangeFilter range,
            @RequestParam(required = false) BookingHistoryDto.MonthBasis basis,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(
                bookingHistoryService.getList(memberId, page, range, basis, year, month)
        );
    }
}
