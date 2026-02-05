package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.response.AdminPaymentResponse;
import com.gateway.paymentgateway.dto.response.AdminUserPaymentSummary;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    // ✅ ALL PAYMENTS (ALL USERS)
    @GetMapping
    public List<AdminPaymentResponse> getAllPayments() {
        return paymentRepository.findAllPaymentsForAdmin();
    }

    // ✅ PAYMENTS BY USER ID
    @GetMapping("/user/{userId}")
    public List<AdminPaymentResponse> getPaymentsByUserId(
            @PathVariable Long userId
    ) {
        return paymentRepository.findPaymentsByUserIdForAdmin(userId);
    }

    // ✅ PAYMENTS BY USER NAME
    @GetMapping("/user")
    public List<AdminPaymentResponse> getPaymentsByUserName(
            @RequestParam String name
    ) {
        return paymentRepository.findPaymentsByUserNameForAdmin(name);
    }

    // ✅ APPROVE REFUND
    @PostMapping("/refund/{paymentId}")
    public String approveRefund(@PathVariable Long paymentId) {
        paymentService.approveAndRefund(paymentId);
        return "Refund processed successfully";
    }
    // ✅ USER-WISE PAYMENT SUMMARY
    @GetMapping("/summary/users")
    public List<AdminUserPaymentSummary> getUserPaymentSummary() {
        return paymentRepository.getUserPaymentSummary();
    }

    // ✅ USER PAYMENT SUMMARY BY EMAIL
    @GetMapping("/summary/users/email")
    public List<AdminUserPaymentSummary> getUserPaymentSummaryByEmail(
            @RequestParam String email
    ) {
        return paymentRepository.getUserPaymentSummaryByEmail(email);
    }
    // ✅ SUMMARY BY USER ID
    @GetMapping("/summary/users/id/{userId}")
    public List<AdminUserPaymentSummary> getUserPaymentSummaryByUserId(
            @PathVariable Long userId
    ) {
        return paymentRepository.getUserPaymentSummaryByUserId(userId);
    }

    // ✅ SUMMARY BY USER NAME (CASE + SPACE INSENSITIVE)
    @GetMapping("/summary/users/name")
    public List<AdminUserPaymentSummary> getUserPaymentSummaryByName(
            @RequestParam String name
    ) {
        return paymentRepository.getUserPaymentSummaryByName(name);
    }
}
