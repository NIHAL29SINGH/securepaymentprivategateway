package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.dto.response.AdminUserPaymentSummary;
import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.dto.response.AdminPaymentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ===============================
    // ✅ USER SIDE (UNCHANGED)
    // ===============================
    List<Payment> findByUserId(Long userId);

    Payment findByRazorpayOrderId(String orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // 🔥 IMPORTANT: ownership check
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Payment p
        WHERE p.id = :paymentId
        AND p.user.email = :email
    """)
    boolean existsByIdAndUserEmail(Long paymentId, String email);

    // ===============================
    // 🔐 ADMIN SIDE (NEW – SAFE)
    // ===============================

    // ✅ ALL PAYMENTS WITH USER DETAILS
    @Query("""
        SELECT new com.gateway.paymentgateway.dto.response.AdminPaymentResponse(
            p.id,
            p.razorpayOrderId,
            p.razorpayPaymentId,
            p.amount,
            p.currency,
            p.status,
            p.refundStatus,
            p.createdAt,
            u.id,
            u.name,
            u.email
        )
        FROM Payment p
        JOIN p.user u
        ORDER BY p.createdAt DESC
    """)
    List<AdminPaymentResponse> findAllPaymentsForAdmin();

    // ✅ PAYMENTS BY USER ID
    @Query("""
        SELECT new com.gateway.paymentgateway.dto.response.AdminPaymentResponse(
            p.id,
            p.razorpayOrderId,
            p.razorpayPaymentId,
            p.amount,
            p.currency,
            p.status,
            p.refundStatus,
            p.createdAt,
            u.id,
            u.name,
            u.email
        )
        FROM Payment p
        JOIN p.user u
        WHERE u.id = :userId
        ORDER BY p.createdAt DESC
    """)
    List<AdminPaymentResponse> findPaymentsByUserIdForAdmin(Long userId);

    // ✅ PAYMENTS BY USER NAME
    @Query("""
        SELECT new com.gateway.paymentgateway.dto.response.AdminPaymentResponse(
            p.id,
            p.razorpayOrderId,
            p.razorpayPaymentId,
            p.amount,
            p.currency,
            p.status,
            p.refundStatus,
            p.createdAt,
            u.id,
            u.name,
            u.email
        )
        FROM Payment p
        JOIN p.user u
        WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY p.createdAt DESC
    """)
    List<AdminPaymentResponse> findPaymentsByUserNameForAdmin(String name);

    @Query("""
    SELECT new com.gateway.paymentgateway.dto.response.AdminUserPaymentSummary(
        u.id,
        u.name,
        u.email,
        COUNT(p.id),
        SUM(CASE WHEN p.status = 'SUCCESS' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'REFUNDED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'CREATED' THEN 1 ELSE 0 END)
    )
    FROM Payment p
    JOIN p.user u
    GROUP BY u.id, u.name, u.email
    ORDER BY u.name
""")
    List<AdminUserPaymentSummary> getUserPaymentSummary();
    // ===============================
// ✅ USER PAYMENT SUMMARY BY USER ID
// ===============================
    @Query("""
    SELECT new com.gateway.paymentgateway.dto.response.AdminUserPaymentSummary(
        u.id,
        u.name,
        u.email,
        COUNT(p.id),
        SUM(CASE WHEN p.status = 'SUCCESS' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'REFUNDED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'CREATED' THEN 1 ELSE 0 END)
    )
    FROM Payment p
    JOIN p.user u
    WHERE u.id = :userId
    GROUP BY u.id, u.name, u.email
""")
    List<AdminUserPaymentSummary> getUserPaymentSummaryByUserId(Long userId);


    // ===============================
// ✅ USER PAYMENT SUMMARY BY NAME
// (CASE + SPACE INSENSITIVE)
// ===============================
    @Query("""
    SELECT new com.gateway.paymentgateway.dto.response.AdminUserPaymentSummary(
        u.id,
        u.name,
        u.email,
        COUNT(p.id),
        SUM(CASE WHEN p.status = 'SUCCESS' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'REFUNDED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'CREATED' THEN 1 ELSE 0 END)
    )
    FROM Payment p
    JOIN p.user u
    WHERE REPLACE(LOWER(u.name), ' ', '') 
          LIKE CONCAT('%', REPLACE(LOWER(:name), ' ', ''), '%')
    GROUP BY u.id, u.name, u.email
    ORDER BY u.name
""")
    List<AdminUserPaymentSummary> getUserPaymentSummaryByName(String name);

    @Query("""
    SELECT new com.gateway.paymentgateway.dto.response.AdminUserPaymentSummary(
        u.id,
        u.name,
        u.email,
        COUNT(p.id),
        SUM(CASE WHEN p.status = 'SUCCESS' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'REFUNDED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN p.status = 'CREATED' THEN 1 ELSE 0 END)
    )
    FROM Payment p
    JOIN p.user u
    WHERE u.email = :email
    GROUP BY u.id, u.name, u.email
""")
    List<AdminUserPaymentSummary> getUserPaymentSummaryByEmail(String email);

    List<Payment> findAllByUserEmailOrderByCreatedAtDesc(String email);

}
