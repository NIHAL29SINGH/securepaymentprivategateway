package com.gateway.paymentgateway.metrices;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final Counter successPayments;
    private final Counter failedPayments;
    private final Counter refundedPayments;   // NEW

    public PaymentMetrics(MeterRegistry registry) {

        this.successPayments = Counter.builder("payments_success_total")
                .description("Total successful payments")
                .register(registry);

        this.failedPayments = Counter.builder("payments_failure_total")
                .description("Total failed payments")
                .register(registry);

        this.refundedPayments = Counter.builder("payments_refunded_total")
                .description("Total refunded payments")
                .register(registry);
    }

    public void paymentSuccess() {
        successPayments.increment();
    }

    public void paymentFailed() {
        failedPayments.increment();
    }

    public void paymentRefunded() {   // NEW METHOD
        refundedPayments.increment();
    }
}
