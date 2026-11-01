package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.dto.request.LoginRequest;
import com.gateway.paymentgateway.entity.EmailVerificationToken;
import com.gateway.paymentgateway.entity.Role;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.exception.ApiException;
import com.gateway.paymentgateway.repository.EmailVerificationTokenRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import com.gateway.paymentgateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final EmailVerificationService tokenService;
    private final EmailService emailService;
    private final GoogleOAuthService googleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationTokenRepository tokenRepo;

    @Value("${admin.email}")
    private String adminEmail;

    // =========================
    // GOOGLE SIGNUP / LOGIN
    // =========================
    public void googleSignup(String idToken) {

        // Verify Google ID token
        var payload = googleService.verify(idToken);

        // Get email from Google
        String email = payload.getEmail();

        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GOOGLE_EMAIL_MISSING",
                    "The Google account does not contain an email address");
        }

        // =========================
        // GET NAME FROM GOOGLE
        // =========================

        String name = (String) payload.get("name");

        // If "name" is not available, use given_name + family_name
        if (name == null || name.isBlank()) {

            String givenName = (String) payload.get("given_name");
            String familyName = (String) payload.get("family_name");

            if (givenName != null && !givenName.isBlank()) {

                if (familyName != null && !familyName.isBlank()) {
                    name = givenName + " " + familyName;
                } else {
                    name = givenName;
                }

            } else {
                // Final fallback
                name = "User";
            }
        }

        System.out.println("Google Email: " + email);
        System.out.println("Google Name: " + name);

        // =========================
        // FIND EXISTING USER
        // =========================

        boolean isNewUser = false;

        User user = userRepo.findByEmail(email).orElse(null);

        if (user == null) {

            user = new User();

            user.setEmail(email);
            user.setName(name);
            user.setRole(Role.USER);
            user.setActive(true);

            isNewUser = true;

        } else {

            // Update name from Google
            user.setName(name);
        }

        // =========================
        // GOOGLE VERIFICATION
        // =========================

        user.setGoogleVerified(true);

        /*
         * Do NOT set emailVerified = true here.
         *
         * The user still has to enter the verification
         * code and set their password using /verify.
         */
        user.setEmailVerified(false);

        userRepo.save(user);

        // =========================
        // SEND VERIFICATION CODE
        // =========================

        if (user.getPassword() == null) {

            EmailVerificationToken token = tokenService.create(user);

            String body =
                    "Hi " + name + ",\n\n" +
                            "Welcome to Payment Gateway!\n\n" +
                            "This is an automated email from your secure payment app.\n\n" +
                            "To finish setting up your account, please use the verification code below:\n\n" +
                            "Verification Code: " + token.getToken() + "\n\n" +
                            "This code expires in 15 minutes.\n\n" +
                            "If you didn't request this, you can safely ignore this email.\n\n" +
                            "Thanks,\n" +
                            "Payment Gateway Team";

            emailService.send(
                    user.getEmail(),
                    "Payment Gateway Verification Code",
                    body
            );
        }

        // =========================
        // ADMIN NOTIFICATION
        // =========================

        if (isNewUser) {

            emailService.send(
                    adminEmail,
                    "New User Registered",
                    "A new user has registered.\n\n" +
                            "Name: " + name + "\n" +
                            "Email: " + email + "\n" +
                            "Google Verified: YES"
            );
        }
    }

    // =========================
    // VERIFY CODE + SET PASSWORD
    // =========================
    public void verifyEmail(String token, String password) {

        // =========================
        // VALIDATE PASSWORD
        // =========================

        if (password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED",
                    "Password cannot be empty");
        }

        // =========================
        // FIND VERIFICATION TOKEN
        // =========================

        EmailVerificationToken verification =
                tokenRepo.findByToken(token)
                        .orElseThrow(() ->
                                new ApiException(HttpStatus.NOT_FOUND, "VERIFICATION_TOKEN_INVALID",
                                        "Verification token is invalid"));

        // =========================
        // CHECK TOKEN USED
        // =========================

        if (verification.isUsed()) {
            throw new ApiException(HttpStatus.CONFLICT, "VERIFICATION_TOKEN_USED",
                    "Verification token has already been used");
        }

        // =========================
        // CHECK TOKEN EXPIRY
        // =========================

        if (verification.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.GONE, "VERIFICATION_TOKEN_EXPIRED",
                    "Verification token has expired");
        }

        // =========================
        // GET USER
        // =========================

        User user = verification.getUser();

        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                    "User associated with the verification token was not found");
        }

        // =========================
        // SET PASSWORD
        // =========================

        user.setPassword(
                passwordEncoder.encode(password)
        );

        // =========================
        // MARK EMAIL VERIFIED
        // =========================

        user.setEmailVerified(true);

        // =========================
        // MARK TOKEN USED
        // =========================

        verification.setUsed(true);

        // =========================
        // SAVE
        // =========================

        userRepo.save(user);
        tokenRepo.save(verification);
    }

    // =========================
    // LOGIN
    // =========================
    public String login(LoginRequest request) {

        // =========================
        // FIND USER
        // =========================

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                                "Invalid email or password"));

        // =========================
        // CHECK EMAIL VERIFICATION
        // =========================

        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Email address has not been verified");
        }

        // =========================
        // CHECK ACCOUNT STATUS
        // =========================

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                    "Account is disabled");
        }

        // =========================
        // CHECK PASSWORD
        // =========================

        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid email or password");
        }

        // =========================
        // CHECK PASSWORD
        // =========================

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid email or password");
        }

        // =========================
        // GENERATE JWT
        // =========================

        return jwtUtil.generateToken(user.getEmail());
    }
}