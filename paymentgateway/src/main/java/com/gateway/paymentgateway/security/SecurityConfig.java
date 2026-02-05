package com.gateway.paymentgateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ❌ Disable CSRF (JWT + APIs)
                .csrf(csrf -> csrf.disable())

                // ❌ Stateless (JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 🔐 Authorization rules
                .authorizeHttpRequests(auth -> auth

                        // ✅ Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ✅ Actuator (Prometheus / Grafana)
                        .requestMatchers("/actuator/**").permitAll()

                        // ✅ Auth APIs
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ Razorpay Webhook (called by Razorpay servers)
                        .requestMatchers("/api/payment/webhook").permitAll()

                        // ✅ Razorpay Checkout UI (NO JWT)
                        .requestMatchers(
                                "/pay",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()

                        // 🔐 Admin APIs
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 🔐 Everything else requires JWT
                        .anyRequest().authenticated()
                )

                // ✅ JWT Filter
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // 🔑 Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
