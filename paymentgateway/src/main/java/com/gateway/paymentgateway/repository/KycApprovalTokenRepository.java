package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.entity.KycApprovalToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycApprovalTokenRepository
        extends JpaRepository<KycApprovalToken, Long> {

    Optional<KycApprovalToken> findByToken(String token);
}
