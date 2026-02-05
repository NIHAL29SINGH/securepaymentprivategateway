package com.gateway.paymentgateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminUserPaymentSummary {

    private Long userId;
    private String userName;
    private String userEmail;

    private Long totalPayments;
    private Long successfulPayments;
    private Long refundedPayments;
    private Long createdPayments;
}
