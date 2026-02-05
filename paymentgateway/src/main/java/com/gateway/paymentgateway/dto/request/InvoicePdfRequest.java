package com.gateway.paymentgateway.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoicePdfRequest {
    private String email;
    private String password;
}
