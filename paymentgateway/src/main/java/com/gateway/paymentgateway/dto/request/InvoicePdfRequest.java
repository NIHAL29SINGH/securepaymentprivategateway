package com.gateway.paymentgateway.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class InvoicePdfRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
}
