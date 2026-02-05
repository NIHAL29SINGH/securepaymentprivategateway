package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.PasswordResetToken;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.PasswordResetTokenRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // ===============================
    // FORGOT PASSWORD
    // ===============================
    public void forgotPassword(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(30));

        tokenRepo.save(resetToken);

        // 📩 SEND EMAIL
        emailService.send(
                user.getEmail(),
                "Reset Your Password",
                "Click the link or use the token below to reset your password:\n\n" +
                        "Token: " + token + "\n\n" +
                        "This token is valid for 30 minutes."
        );
    }

    // ===============================
    // RESET PASSWORD
    // ===============================
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token already used");
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        resetToken.setUsed(true);

        userRepo.save(user);
        tokenRepo.save(resetToken);
    }
}
