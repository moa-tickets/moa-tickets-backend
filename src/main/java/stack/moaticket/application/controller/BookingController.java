package stack.moaticket.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BookingDto;
import stack.moaticket.application.service.BookingService;

import java.util.List;

@Tag(name = "Booking API", description = "상품(콘서트)예약 도메인 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    // 회차별 좌석 목록 조회 (좌석 배치도용)
    // HOLD 상태라도 holdExpired가 지난 좌석은 AVAILABLE로 내려준다
    @Operation(
            security = {},
            summary = "회차별 좌석 목록 Read",
            description = "sessionId를 받으면 해당 세션에 맞는 좌석(티켓)들을 조회",
            parameters = {
                    @Parameter(
                            name = "sessionId",
                            description = "세션 ID",
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
                                    array = @ArraySchema(schema = @Schema(implementation = BookingDto.TicketResponse.class))
                            )
                    )
            }
    )
    @GetMapping("/sessions/{sessionId}/tickets")
    public ResponseEntity<List<BookingDto.TicketResponse>> getTicketsBySession(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(bookingService.getTicketsBySession(sessionId));
    }

    // 좌석 임시 점유 (HOLD)
    // Request: { sessionId, ticketIds(최대 4개) }
    // Response: { holdToken, expiresAt }
    @Operation(
            summary = "좌석 임시 점유",
            description = "sessionId, ticketIds(최대 4개)를 입력 받아 해당 세션과 티켓들에 임시로 tickethold 생성",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "sessionId, List<ticketIds>(최대 4개)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BookingDto.HoldRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BookingDto.HoldResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "티켓 정보를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청값이 올바르지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "이미 판매된 좌석"
                    )
            }

    )
    @PostMapping("/tickets/hold")
    public ResponseEntity<BookingDto.HoldResponse> holdTickets(
            @AuthenticationPrincipal Long memberId,
            @RequestBody BookingDto.HoldRequest request) {
        BookingService.HoldResult result =
                bookingService.holdTickets(memberId, request.getSessionId(), request.getTicketIds());

        BookingDto.HoldResponse response = BookingDto.HoldResponse.builder()
                .holdToken(result.holdToken())
                .expiresAt(result.expiresAt())
                .build();

        return ResponseEntity.ok(response);
    }

    //점유 해제 (HOLD -> AVAILABLE)
    // 이미 만료되어 AVAILABLE 상태여도 성공(200)으로 처리
    @Operation(
            summary = "좌석 점유 해제(HOLD > AVAILABLE)",
            description = "좌석 ticketHold의 점유 상태를 변경",
            parameters = {
                    @Parameter(
                            name = "holdToken",
                            description = "점유 해제할 holdToken",
                            required = true,
                            in = ParameterIn.PATH
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한이 없음"
                    )
            }

    )
    @PostMapping("/holds/{holdToken}/release")
    public ResponseEntity<Void> releaseHold(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String holdToken) {
        bookingService.releaseHold(memberId, holdToken);
        return ResponseEntity.ok().build();
    }
}