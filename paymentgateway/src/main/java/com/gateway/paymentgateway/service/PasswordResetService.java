package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.PasswordResetToken;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.exception.ApiException;
import com.gateway.paymentgateway.repository.PasswordResetTokenRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "User not found"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(30));

        tokenRepo.save(resetToken);

        //  SEND EMAIL
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESET_TOKEN_INVALID",
                        "Password reset token is invalid"));

        if (resetToken.isUsed()) {
            throw new ApiException(HttpStatus.CONFLICT, "RESET_TOKEN_USED",
                    "Password reset token has already been used");
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.GONE, "RESET_TOKEN_EXPIRED",
                    "Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        resetToken.setUsed(true);

        userRepo.save(user);
        tokenRepo.save(resetToken);
    }
}
