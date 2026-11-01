package com.gateway.paymentgateway.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.gateway.paymentgateway.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayClient razorpayClient;

    public String createOrder(Double amount) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount * 100);
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);
            return order.get("id");

        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR",
                    "The payment provider could not create the order", e);
        }
    }
}
