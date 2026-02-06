package com.gateway.paymentgateway.metrices;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import com.gateway.paymentgateway.repository.UserRepository;

@Component
public class UserMetrics {

    public UserMetrics(MeterRegistry registry, UserRepository userRepository) {

        Gauge.builder("total_users", userRepository, UserRepository::count)
                .description("Total registered users")
                .register(registry);
    }
}
