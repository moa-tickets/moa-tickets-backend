package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.service.PaymentService;
import stack.moaticket.domain.member.entity.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 준비 (holdToken 검증 -> amount/orderName/orderId 생성 -> Payment(READY) 저장)
    @PostMapping("/prepare")
    public ResponseEntity<PaymentDto.PrepareResponse> prepare(
            @AuthenticationPrincipal Member member,
            @RequestBody PaymentDto.PrepareRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.prepare(member.getId(), request)
        );
    }
}
