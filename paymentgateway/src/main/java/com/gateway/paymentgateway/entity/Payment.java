package com.gateway.paymentgateway.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "idempotencyKey")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    private Double amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;

    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundedAt;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private LocalDateTime createdAt;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;
}
