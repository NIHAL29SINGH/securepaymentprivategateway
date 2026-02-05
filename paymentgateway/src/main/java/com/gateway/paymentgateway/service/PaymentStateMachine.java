package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.PaymentStatus;

public final class PaymentStateMachine {

    private PaymentStateMachine() {}

    public static void validate(
            PaymentStatus current,
            PaymentStatus next
    ) {
        if (current == null) {
            throw new IllegalStateException("Current payment state is null");
        }

        switch (current) {

            case CREATED -> {
                // Razorpay webhook may jump directly to CAPTURED
                if (next != PaymentStatus.PAYMENT_INITIATED &&
                        next != PaymentStatus.AUTHORIZED &&
                        next != PaymentStatus.CAPTURED &&
                        next != PaymentStatus.FAILED) {
                    invalid(current, next);
                }
            }

            case PAYMENT_INITIATED -> {
                if (next != PaymentStatus.AUTHORIZED &&
                        next != PaymentStatus.FAILED) {
                    invalid(current, next);
                }
            }

            case AUTHORIZED -> {
                if (next != PaymentStatus.CAPTURED) {
                    invalid(current, next);
                }
            }

            case CAPTURED -> {
                if (next != PaymentStatus.SUCCESS) {
                    invalid(current, next);
                }
            }

            case SUCCESS -> {
                if (next != PaymentStatus.REFUND_REQUESTED) {
                    invalid(current, next);
                }
            }

            case REFUND_REQUESTED -> {
                if (next != PaymentStatus.REFUNDED &&
                        next != PaymentStatus.REFUND_REJECTED) {
                    invalid(current, next);
                }
            }

            case FAILED, REFUNDED, REFUND_REJECTED -> {
                throw new IllegalStateException(
                        "No transitions allowed from final state: " + current
                );
            }
        }
    }

    private static void invalid(
            PaymentStatus current,
            PaymentStatus next
    ) {
        throw new IllegalStateException(
                "Invalid payment state transition: " +
                        current + " → " + next
        );
    }
}
