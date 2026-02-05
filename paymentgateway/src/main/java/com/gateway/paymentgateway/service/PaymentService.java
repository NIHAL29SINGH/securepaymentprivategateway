package com.gateway.paymentgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.paymentgateway.entity.*;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private static final String IDEM_PREFIX = "idem:";

    // ✅ ADMIN EMAIL (as requested)
    private static final String ADMIN_EMAIL = "nihallassiyt69@gmail.com";

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;
    private final UserKycRepository kycRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Counter paymentSuccessCounter;
    private final Counter paymentFailureCounter;

    @Value("${razorpay.key.id}")
    private String razorpayKey;

    public PaymentService(
            RazorpayClient razorpayClient,
            PaymentRepository paymentRepo,
            UserRepository userRepo,
            UserKycRepository kycRepo,
            RedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry,
            EmailService emailService
    ) {
        this.razorpayClient = razorpayClient;
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.kycRepo = kycRepo;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;

        this.paymentSuccessCounter =
                meterRegistry.counter("payments.success");
        this.paymentFailureCounter =
                meterRegistry.counter("payments.failure");
    }

    // =========================================
    // 🔐 STATE MACHINE GUARD
    // =========================================
    private void updateStatus(Payment payment, PaymentStatus next) {
        PaymentStateMachine.validate(payment.getStatus(), next);
        payment.setStatus(next);
    }

    // =========================================
    // ✅ CREATE PAYMENT
    // =========================================
    public Map<String, Object> createPayment(
            String email,
            Double amount,
            String idempotencyKey
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new RuntimeException("Idempotency-Key header is required");
        }

        String redisKey = IDEM_PREFIX + idempotencyKey;

        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                return objectMapper.readValue(
                        cached,
                        new TypeReference<>() {}
                );
            }
        } catch (Exception ignored) {}

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("KYC not submitted"));

        if (kyc.getStatus() != KycStatus.APPROVED) {
            throw new RuntimeException("KYC not approved");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", (int) (amount * 100));
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);

            Payment payment = new Payment();
            payment.setUser(user);
            payment.setAmount(amount);
            payment.setCurrency("INR");
            payment.setRazorpayOrderId(order.get("id"));
            payment.setStatus(PaymentStatus.CREATED);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setIdempotencyKey(idempotencyKey);

            paymentRepo.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", "INR");
            response.put("razorpayKey", razorpayKey);

            redisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(response),
                    Duration.ofMinutes(15)
            );

            return response;

        } catch (Exception e) {
            paymentFailureCounter.increment();
            throw new RuntimeException("Razorpay order creation failed", e);
        }
    }

    // =========================================
    // ✅ WEBHOOK SUCCESS
    // =========================================
    public void markPaymentSuccess(String orderId, String paymentId) {

        Payment payment = paymentRepo.findByRazorpayOrderId(orderId);

        if (payment == null) {
            throw new RuntimeException("Payment not found");
        }

        payment.setRazorpayPaymentId(paymentId);

        if (payment.getStatus() == PaymentStatus.CREATED) {
            payment.setStatus(PaymentStatus.CAPTURED);
        }

        updateStatus(payment, PaymentStatus.SUCCESS);
        paymentRepo.save(payment);

        paymentSuccessCounter.increment();
    }

    // =========================================
    // ✅ ADMIN APPROVE REFUND
    // =========================================
    public void approveAndRefund(Long paymentId) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new RuntimeException("Refund not requested");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", (int) (payment.getAmount() * 100));

            razorpayClient.payments.refund(
                    payment.getRazorpayPaymentId(),
                    options
            );

            updateStatus(payment, PaymentStatus.REFUNDED);
            payment.setRefundStatus(RefundStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());

            paymentRepo.save(payment);

            emailService.send(
                    payment.getUser().getEmail(),
                    "Refund Completed",
                    "Refund completed for Order ID: "
                            + payment.getRazorpayOrderId()
            );

        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay refund failed", e);
        }
    }

    // =========================================
    // ❌ ADMIN REJECT REFUND
    // =========================================
    public void rejectRefund(Long paymentId, String reason) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new RuntimeException("Refund not in requested state");
        }

        updateStatus(payment, PaymentStatus.REFUND_REJECTED);
        payment.setRefundStatus(RefundStatus.REJECTED);

        paymentRepo.save(payment);

        emailService.send(
                payment.getUser().getEmail(),
                "Refund Rejected",
                "Reason: " + reason
        );
    }

    // =========================================
    // ✅ USER REQUEST REFUND (IDEMPOTENT + ADMIN EMAIL)
    // =========================================
    public void requestRefund(Long paymentId, String userEmail) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized refund request");
        }

        if (payment.getRazorpayPaymentId() == null) {
            throw new RuntimeException("Refund not allowed for unpaid payment");
        }

        if (payment.getStatus() == PaymentStatus.REFUND_REQUESTED
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        updateStatus(payment, PaymentStatus.REFUND_REQUESTED);
        payment.setRefundStatus(RefundStatus.REQUESTED);

        paymentRepo.save(payment);

        // 📧 Email to USER
        emailService.send(
                userEmail,
                "Refund Requested",
                "Your refund request for Order ID "
                        + payment.getRazorpayOrderId()
                        + " has been sent to admin."
        );

        // 📧 Email to ADMIN
        emailService.send(
                ADMIN_EMAIL,
                "New Refund Request",
                "User: " + userEmail
                        + "\nOrder ID: " + payment.getRazorpayOrderId()
                        + "\nAmount: ₹" + payment.getAmount()
        );
    }

    // =========================================
    // ✅ GET PAYMENT BY ORDER ID
    // =========================================
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepo.findByRazorpayOrderId(orderId);
    }

    public String getRazorpayKey() {
        return razorpayKey;
    }


    @Transactional(readOnly = true)
    public void sendInvoicePdf(
            String email,
            String rawPassword,
            PasswordEncoder passwordEncoder
    ) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        List<Payment> payments =
                paymentRepo.findAllByUserEmailOrderByCreatedAtDesc(email);

        if (payments.isEmpty()) {
            throw new RuntimeException("No payments found");
        }

        long success = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .count();

        long refunded = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
                .count();

        double totalPaid = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .mapToDouble(Payment::getAmount)
                .sum();

        // =====================
        // 📄 CREATE PDF
        // =====================
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 11);

        document.add(new Paragraph("PAYMENT INVOICE", titleFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Name: " + user.getName(), normalFont));
        document.add(new Paragraph("Email: " + user.getEmail(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total Payments: " + payments.size(), normalFont));
        document.add(new Paragraph("Successful Payments: " + success, normalFont));
        document.add(new Paragraph("Refunded Payments: " + refunded, normalFont));
        document.add(new Paragraph("Total Amount Paid: ₹" + totalPaid, normalFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("TRANSACTION DETAILS", titleFont));
        document.add(new Paragraph(" "));

        for (Payment p : payments) {
            document.add(new Paragraph("--------------------------------------"));
            document.add(new Paragraph("Order ID: " + p.getRazorpayOrderId()));
            document.add(new Paragraph("Amount: ₹" + p.getAmount()));
            document.add(new Paragraph("Status: " + p.getStatus()));
            document.add(new Paragraph("Refund Status: " + p.getRefundStatus()));
            document.add(new Paragraph("Date: " + p.getCreatedAt()));
        }

        document.close();

        // =====================
        // 📧 SEND EMAIL WITH PDF
        // =====================
        emailService.sendWithAttachment(
                email,
                "Your Payment Invoice",
                "Please find attached your payment invoice.",
                outputStream.toByteArray(),
                "invoice.pdf"
        );
    }


}
