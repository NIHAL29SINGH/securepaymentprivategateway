package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/payments")
@RequiredArgsConstructor
public class UserPaymentController {

    private final PaymentRepository paymentRepo;
    private final UserRepository userRepository;

    @GetMapping
    public List<Payment> getMyPayments(
            @AuthenticationPrincipal UserDetails principal
    ) {
        // 🔑 Email comes from JWT
        String email = principal.getUsername();

        // 🔁 Convert UserDetails → User entity
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 📦 Fetch only logged-in user's payments
        return paymentRepo.findByUserId(user.getId());
    }
}
