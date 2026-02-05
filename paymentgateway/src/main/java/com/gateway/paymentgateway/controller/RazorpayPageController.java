package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class RazorpayPageController {

    private final PaymentService paymentService;

    // ✅ PAY EXISTING ORDER
    @GetMapping("/pay")
    public String payExistingOrder(
            @RequestParam("orderId") String orderId,
            Model model
    ) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);

        if (payment == null) {
            throw new RuntimeException("Invalid order ID");
        }

        model.addAttribute("orderId", payment.getRazorpayOrderId());
        model.addAttribute("amount", (int) (payment.getAmount() * 100)); // paise
        model.addAttribute("razorpayKey", paymentService.getRazorpayKey());

        return "razorpay-checkout";
    }
}
