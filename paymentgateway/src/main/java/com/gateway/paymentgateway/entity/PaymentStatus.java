package com.gateway.paymentgateway.entity;

public enum PaymentStatus {

    CREATED,
    PAYMENT_INITIATED,
    AUTHORIZED,
    CAPTURED,
    SUCCESS,
    FAILED,

    REFUND_REQUESTED,
    REFUND_REJECTED,
    REFUNDED
}
