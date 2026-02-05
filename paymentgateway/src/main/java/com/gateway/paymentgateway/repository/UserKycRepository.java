package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.entity.UserKyc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserKycRepository extends JpaRepository<UserKyc, Long> {
    Optional<UserKyc> findByUserId(Long userId);
}
