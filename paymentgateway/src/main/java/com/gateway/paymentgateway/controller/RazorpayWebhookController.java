package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public String handleWebhook(
            @RequestBody String payload,
            HttpServletRequest request
    ) throws Exception {

        String razorpaySignature = request.getHeader("X-Razorpay-Signature");

        if (!verifySignature(payload, razorpaySignature, webhookSecret)) {
            throw new RuntimeException("Invalid webhook signature");
        }

        JSONObject json = new JSONObject(payload);
        String event = json.getString("event");

        if ("payment.captured".equals(event)) {
            JSONObject payment = json
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String orderId = payment.getString("order_id");
            String paymentId = payment.getString("id");

            paymentService.markPaymentSuccess(orderId, paymentId);
        }

        return "OK";
    }

    private boolean verifySignature(
            String payload,
            String actualSignature,
            String secret
    ) throws Exception {

        Mac sha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        sha256.init(key);

        String expectedSignature = Hex.encodeHexString(
                sha256.doFinal(payload.getBytes(StandardCharsets.UTF_8))
        );

        return expectedSignature.equals(actualSignature);
    }
}
