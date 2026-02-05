package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.dto.request.KycRequest;
import com.gateway.paymentgateway.entity.KycApprovalToken;
import com.gateway.paymentgateway.entity.KycStatus;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.entity.UserKyc;
import com.gateway.paymentgateway.repository.KycApprovalTokenRepository;
import com.gateway.paymentgateway.repository.UserKycRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycService {

    private final UserKycRepository kycRepository;
    private final KycApprovalTokenRepository tokenRepository;
    private final EmailService emailService;

    /**
     * Submit KYC by user
     */
    public void submitKyc(User user, KycRequest request) {

        // Prevent duplicate approved KYC
        UserKyc kyc = kycRepository
                .findByUserId(user.getId())
                .orElse(new UserKyc());

        if (kyc.getStatus() == KycStatus.APPROVED) {
            throw new RuntimeException("KYC already approved");
        }

        kyc.setUser(user);
        kyc.setFatherName(request.getFatherName());
        kyc.setDob(request.getDob());
        kyc.setAddress(request.getAddress());
        kyc.setPanNumber(request.getPanNumber());
        kyc.setBankAccount(request.getBankAccount());
        kyc.setIfsc(request.getIfsc());
        kyc.setStatus(KycStatus.PENDING);

        kycRepository.save(kyc);

        // Create admin approval token
        KycApprovalToken token = new KycApprovalToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setUsed(false);
        token.setExpiry(LocalDateTime.now().plusHours(24));

        tokenRepository.save(token);

        // Send email to admin
        emailService.send(
                "nihalsingh2950@system.com",
                "KYC Approval Required",
                "Approve KYC for user: " + user.getEmail() +
                        "\n\nClick here:\n" +
                        "http://localhost:8080/api/admin/kyc/approve?token=" + token.getToken()
        );
    }
}
