package com.gateway.paymentgateway.config;

import com.gateway.paymentgateway.entity.Role;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            System.out.println("✅ Admin already exists");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setEmailVerified(true);
        admin.setGoogleVerified(false);
        admin.setName("System Admin");

        userRepository.save(admin);

        System.out.println("✅ Admin account created successfully");
    }
}
