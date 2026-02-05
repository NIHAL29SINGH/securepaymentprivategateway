package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.dto.response.AdminUserResponse;
import com.gateway.paymentgateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT new com.gateway.paymentgateway.dto.response.AdminUserResponse(
            u.id,
            u.email,
            u.name,
            u.emailVerified,
            COUNT(t.id)
        )
        FROM User u
        LEFT JOIN Transaction t ON t.user = u
        GROUP BY u.id
    """)
    List<AdminUserResponse> getAllUsersWithTransactionCount();
}
