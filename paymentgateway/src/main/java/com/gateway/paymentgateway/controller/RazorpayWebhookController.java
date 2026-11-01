package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class RazorpayWebhookController {

    private final PaymentService paymentService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public String handleWebhook(
            @RequestBody String payload,
            HttpServletRequest request
    ) throws Exception {

        // =========================================
        // GET RAZORPAY SIGNATURE
        // =========================================
        String razorpaySignature =
                request.getHeader("X-Razorpay-Signature");

        // =========================================
        // VERIFY WEBHOOK SIGNATURE
        // =========================================
        if (!verifySignature(
                payload,
                razorpaySignature,
                webhookSecret
        )) {

            throw new RuntimeException(
                    "Invalid webhook signature"
            );
        }

        String eventKey = "webhook:" + sha256(payload);
        Boolean firstDelivery = redisTemplate.opsForValue()
                .setIfAbsent(eventKey, "processed", Duration.ofDays(7));
        if (Boolean.FALSE.equals(firstDelivery)) {
            return "OK";
        }

        JSONObject json = new JSONObject(payload);

        String event = json.getString("event");

        // =========================================
        // PAYMENT CAPTURED / SUCCESS
        // =========================================
        if ("payment.captured".equals(event)) {

            JSONObject payment = json
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String orderId =
                    payment.getString("order_id");

            String paymentId =
                    payment.getString("id");

            paymentService.markPaymentSuccess(
                    orderId,
                    paymentId
            );
        }

        // =========================================
        // PAYMENT FAILED
        // =========================================
        else if ("payment.failed".equals(event)) {

            JSONObject payment = json
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String orderId =
                    payment.getString("order_id");

            String paymentId =
                    payment.getString("id");

            paymentService.markPaymentFailed(
                    orderId,
                    paymentId
            );
        }

        // =========================================
        // OTHER EVENTS
        // =========================================
        // Ignore events that this controller
        // doesn't need to process.

        return "OK";
    }

    // =========================================
    // VERIFY RAZORPAY WEBHOOK SIGNATURE
    // =========================================
    private boolean verifySignature(
            String payload,
            String actualSignature,
            String secret
    ) throws Exception {

        if (actualSignature == null ||
                actualSignature.isBlank()) {

            return false;
        }

        Mac sha256 =
                Mac.getInstance("HmacSHA256");

        SecretKeySpec key =
                new SecretKeySpec(
                        secret.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        "HmacSHA256"
                );

        sha256.init(key);

        String expectedSignature =
                Hex.encodeHexString(
                        sha256.doFinal(
                                payload.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                );

        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                actualSignature.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) result.append(String.format("%02x", item));
        return result.toString();
    }
}