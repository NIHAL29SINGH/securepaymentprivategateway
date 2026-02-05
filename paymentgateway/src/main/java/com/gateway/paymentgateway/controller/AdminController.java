package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.response.KycAdminResponse;
import com.gateway.paymentgateway.dto.response.UserResponse;
import com.gateway.paymentgateway.entity.Payment;
import com.gateway.paymentgateway.service.PaymentService;


import com.gateway.paymentgateway.repository.PaymentRepository;
import com.gateway.paymentgateway.service.AdminService;
import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ✅ THIS WAS MISSING


import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PaymentService paymentService;

    private final AdminService adminService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/email/{email}")
    public UserResponse getByEmail(@PathVariable String email) {
        return adminService.getByEmail(email);
    }

    @GetMapping("/users/id/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return adminService.getById(id);
    }

    @PutMapping("/users/{id}/disable")
    public String disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return "User disabled";
    }

    @PutMapping("/users/{id}/enable")
    public String enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return "User enabled";
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return "User deleted";
    }

    // ✅ FIXED KYC ENDPOINT
    @GetMapping("/kyc/{userId}")
    public KycAdminResponse getUserKyc(@PathVariable Long userId) {
        return adminService.getUserKyc(userId);
    }

    @PostMapping("/kyc/approve/{userId}")
    public String approveKyc(@PathVariable Long userId) {
        adminService.approveKycByUserId(userId);
        return "KYC approved successfully";
    }

    @PostMapping("/kyc/reject/{userId}")
    public String rejectKyc(
            @PathVariable Long userId,
            @RequestParam String reason
    ) {
        adminService.rejectKyc(userId, reason);
        return "KYC rejected";
    }




}
