package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.dto.request.KycRequest;
import com.gateway.paymentgateway.entity.KycStatus;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.entity.UserKyc;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final UserKycRepository kycRepo;
    private final EmailService emailService;

    @Value("${admin.email}")
    private String adminEmail;

    // ============================
    // GET USER PROFILE
    // ============================
    public Map<String, Object> getProfile(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(user.getId()).orElse(null);

        boolean approved = kyc != null && kyc.getStatus() == KycStatus.APPROVED;

        Map<String, Object> response = new HashMap<>();

        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("emailVerified", user.isEmailVerified());
        response.put("googleVerified", user.isGoogleVerified());
        response.put("createdAt", user.getCreatedAt());
        response.put("kycStatus",
                kyc == null ? KycStatus.NOT_SUBMITTED : kyc.getStatus());

        Map<String, Object> profile = new HashMap<>();

        if (approved) {
            profile.put("fatherName", kyc.getFatherName());
            profile.put("dob", kyc.getDob());
            profile.put("address", kyc.getAddress());
            profile.put("panNumber", kyc.getPanNumber());
            profile.put("bankAccount", kyc.getBankAccount());
            profile.put("ifsc", kyc.getIfsc());
        }

        response.put("profile", profile);
        response.put("canEditKyc", kyc == null || kyc.getStatus() != KycStatus.APPROVED);
        response.put("canMakePayment", approved);

        return response;
    }

    // ============================
    // SUBMIT KYC
    // ============================
    public String submitKyc(String email, KycRequest request) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(user.getId())
                .orElse(new UserKyc());

        kyc.setUser(user);
        kyc.setFatherName(request.getFatherName());
        kyc.setDob(request.getDob());
        kyc.setAddress(request.getAddress());
        kyc.setPanNumber(request.getPanNumber());
        kyc.setBankAccount(request.getBankAccount());
        kyc.setIfsc(request.getIfsc());
        kyc.setStatus(KycStatus.PENDING);

        kycRepo.save(kyc);

        // 📩 Admin Notification
        emailService.send(
                adminEmail,
                "New KYC Submitted",
                "User: " + user.getEmail() +
                        "\nName: " + user.getName() +
                        "\n\nLogin to approve KYC."
        );

        // 📩 User Notification
        emailService.send(
                user.getEmail(),
                "KYC Submitted",
                "Your KYC has been submitted and is under review."
        );

        return "KYC submitted successfully. Awaiting admin approval.";
    }
}
