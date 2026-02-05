package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    // ✅ USER PROVIDES IDEMPOTENCY KEY
    @PostMapping("/create")
    public Map<String, Object> createPayment(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam Double amount
    ) {
        return paymentService.createPayment(
                principal.getUsername(),
                amount,
                idempotencyKey
        );
    }

    @GetMapping("/history")
    public List<Payment> getMyPayments(
            @AuthenticationPrincipal UserDetails principal
    ) {
        var user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return paymentRepository.findByUserId(user.getId());
    }

    @PostMapping("/refund/request")
    public String requestRefund(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long paymentId
    ) {
        paymentService.requestRefund(paymentId, principal.getUsername());
        return "Refund request sent to admin";
    }
}
