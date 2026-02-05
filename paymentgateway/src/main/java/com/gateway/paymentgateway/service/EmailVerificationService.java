package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.entity.EmailVerificationToken;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepo;

    public EmailVerificationToken create(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryTime(LocalDateTime.now().plusMinutes(15));
        token.setUsed(false);
        return tokenRepo.save(token);
    }
}

