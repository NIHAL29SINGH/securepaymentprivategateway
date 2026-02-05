package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.request.KycRequest;
import com.gateway.paymentgateway.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ============================
    // GET PROFILE
    // ============================
    @GetMapping("/profile")
    public Map<String, Object> getProfile(
            @AuthenticationPrincipal UserDetails principal
    ) {
        return userService.getProfile(principal.getUsername());
    }

    // ============================
    // SUBMIT KYC
    // ============================
    @PostMapping("/kyc")
    public String submitKyc(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody KycRequest request
    ) {
        return userService.submitKyc(principal.getUsername(), request);
    }
}
