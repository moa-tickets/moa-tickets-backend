package stack.moaticket.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.BookingDto;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.service.PaymentService;
import stack.moaticket.domain.member.entity.Member;

@Tag(name = "Payment API", description = "상품 결제 도메인 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 준비 (holdToken 검증 -> amount/orderName/orderId 생성 -> Payment(READY) 저장)
    @Operation(
            summary = "Payment(READY)생성",
            description = "구매하려는 token을 검증하고 payment생성",
            requestBody = @RequestBody(
                    description = "구매하려는 ticket을 점유한 holdToken",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentDto.PrepareRequest.class)
                    )
            ),
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
                            responseCode = "404",
                            description = "티켓 정보를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "세션 정보를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청값이 올바르지 않음"
                    )
            }
    )
    @PostMapping("/prepare")
    public ResponseEntity<PaymentDto.PrepareResponse> prepare(
            @AuthenticationPrincipal Member member,
            @RequestBody PaymentDto.PrepareRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.prepare(member.getId(), request)
        );
    }

    // 결제 승인 (Toss confirm 호출)
    // 성공시 Payment = PAID, Ticket = SOLD, PaymentTicket 생성, hold 정리
    // 실패시 MoaException
    @Operation(
            summary = "결제 승인",
            description = "준비된 결제를 토스를 이용해 결제를 진행",
            requestBody = @RequestBody(
                    description = "구매하려는 ticket들의 orderId, paymentKey, amount",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentDto.ConfirmRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공시, 티켓들을 락으로 조회 후 SOLD처리," +
                                    "payment_ticket 생성, " +
                                    "hold 정리, " +
                                    "payment (READY > PAID)"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청값이 올바르지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력 매개변수가 올바르지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한이 없음"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "티켓 정보를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "결제 정보를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "세션 정보를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "결제상태가 유효하지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "결제 금액이 일치하지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "이미 판매된 좌석"
                    ),
                    @ApiResponse(
                            responseCode = "410",
                            description = "좌석 선점시간 만료"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류"
                    )

            }
    )
    @PostMapping("/confirm")
    public ResponseEntity<PaymentDto.ConfirmResponse> confirm(
            @AuthenticationPrincipal Member member,
            @RequestBody PaymentDto.ConfirmRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.confirm(member.getId(), request)
        );
    }
}
