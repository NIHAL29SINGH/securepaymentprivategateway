package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.request.LoginRequest;
import com.gateway.paymentgateway.service.AuthService;
import com.gateway.paymentgateway.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    // ===============================
    // GOOGLE SIGNUP / LOGIN
    // ===============================
    @PostMapping("/google")
    public String googleSignup(@RequestBody Map<String, String> req) {

        authService.googleSignup(req.get("idToken"));
        return "Verification email sent";
    }

    // ===============================
    // VERIFY EMAIL + SET PASSWORD
    // ===============================
    @PostMapping("/verify")
    public String verifyEmail(
            @RequestParam String token,
            @RequestBody Map<String, String> body
    ) {

        authService.verifyEmail(token, body.get("password"));
        return "Account verified successfully";
    }

    // ===============================
    // LOGIN
    // ===============================
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest req) {

        return authService.login(req);
    }

    // ===============================
    // FORGOT PASSWORD
    // ===============================
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email) {

        passwordResetService.forgotPassword(email);
        return "Password reset token sent to email";
    }

    // ===============================
    // RESET PASSWORD
    // ===============================
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {

        passwordResetService.resetPassword(token, newPassword);
        return "Password reset successful";
    }
}
