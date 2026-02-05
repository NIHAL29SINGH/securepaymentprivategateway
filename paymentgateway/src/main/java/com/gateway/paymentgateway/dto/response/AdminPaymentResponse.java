package com.gateway.paymentgateway.dto.response;

import com.gateway.paymentgateway.entity.PaymentStatus;
import com.gateway.paymentgateway.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminPaymentResponse {

    private Long paymentId;

    private String orderId;
    private String paymentIdRazorpay;

    private Double amount;
    private String currency;

    private PaymentStatus status;
    private RefundStatus refundStatus;

    private LocalDateTime createdAt;

    // 👤 USER INFO (SAFE)
    private Long userId;
    private String userName;
    private String userEmail;
}
