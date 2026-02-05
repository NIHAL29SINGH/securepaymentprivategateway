package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.dto.response.KycAdminResponse;
import com.gateway.paymentgateway.dto.response.UserResponse;
import com.gateway.paymentgateway.entity.*;
import com.gateway.paymentgateway.repository.KycApprovalTokenRepository;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepo;
    private final UserKycRepository kycRepo;
    private final KycApprovalTokenRepository tokenRepo;
    private final EmailService emailService;

    // ================= USER =================

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getByEmail(String email) {
        return UserResponse.from(
                userRepo.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(
                userRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
    }

    public void disableUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepo.save(user);
    }

    public void enableUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepo.save(user);
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    // ================= KYC =================

    public KycAdminResponse getUserKyc(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        KycAdminResponse res = new KycAdminResponse();
        res.setUserId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setFatherName(kyc.getFatherName());
        res.setDob(kyc.getDob());
        res.setAddress(kyc.getAddress());
        res.setPanNumber(kyc.getPanNumber());
        res.setBankAccount(kyc.getBankAccount());
        res.setIfsc(kyc.getIfsc());
        res.setStatus(kyc.getStatus());

        return res;
    }

    public void approveKycByUserId(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        if (kyc.getStatus() == KycStatus.APPROVED)
            throw new RuntimeException("Already approved");

        kyc.setStatus(KycStatus.APPROVED);
        user.setActive(true);

        kycRepo.save(kyc);
        userRepo.save(user);

        emailService.send(
                user.getEmail(),
                "KYC Approved",
                "Your KYC is approved. You can now make payments."
        );
    }

    public void rejectKyc(Long userId, String reason) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserKyc kyc = kycRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        kyc.setStatus(KycStatus.REJECTED);
        kycRepo.save(kyc);

        emailService.send(
                user.getEmail(),
                "KYC Rejected",
                "Reason: " + reason
        );
    }
}
