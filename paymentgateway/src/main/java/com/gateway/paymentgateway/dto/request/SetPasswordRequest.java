package com.gateway.paymentgateway.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SetPasswordRequest {
    @NotBlank
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;
}
