package com.gateway.paymentgateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                //  Disable CSRF (JWT + APIs)
                .csrf(csrf -> csrf.disable())

                //  Stateless (JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //  Authorization rules
                .authorizeHttpRequests(auth -> auth

                        //  Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Keep health public for orchestration; protect metrics and other endpoints.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN")

                        //  Auth APIs
                        .requestMatchers("/api/auth/**").permitAll()

                        //  Razorpay Webhook (called by Razorpay servers)
                        .requestMatchers("/api/payment/webhook").permitAll()

                        //  Razorpay Checkout UI (NO JWT)
                        .requestMatchers(
                                "/pay",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()

                        //  Admin APIs
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        //  Everything else requires JWT
                        .anyRequest().authenticated()
                )

                //  JWT Filter
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    //  Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"about:blank","title":"Authentication required","status":401,
                    "detail":"A valid Bearer token is required","code":"AUTHENTICATION_REQUIRED",
                    "path":"%s","timestamp":"%s"}"""
                    .formatted(request.getRequestURI(), OffsetDateTime.now()));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"about:blank","title":"Access denied","status":403,
                    "detail":"You do not have permission to access this resource","code":"FORBIDDEN",
                    "path":"%s","timestamp":"%s"}"""
                    .formatted(request.getRequestURI(), OffsetDateTime.now()));
        };
    }
}
